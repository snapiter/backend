package com.snapiter.backend.integration

import com.snapiter.backend.api.trackable.CreateTrackableRequest
import com.snapiter.backend.api.trackable.CreateTripRequest
import com.snapiter.backend.api.trackable.PositionRequest
import com.snapiter.backend.api.trackable.QuickCreateRes
import com.snapiter.backend.api.trackable.RegisterDevice
import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.trip.PositionType
import com.snapiter.backend.model.users.User
import com.snapiter.backend.model.users.UserRepository
import com.snapiter.backend.security.JwtService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient(timeout = "PT30S")
@Testcontainers
class DeviceTripPositionsFlowIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17"))

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            // App runtime talks R2DBC...
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            // ...while Flyway needs a JDBC url. application.yml sets an explicit spring.flyway.url
            // (localhost) which would otherwise win, so we must point Flyway at the container too.
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
        }
    }

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var jwtService: JwtService

    @Test
    fun `should retrieve positions after registering a device and posting positions`() {
        // --- Seed: a user directly (the trackable's user_id FKs users, and there is no
        // create-user API in this flow); everything else goes through the real HTTP API. ---
        val userId = UUID.randomUUID()
        val unique = UUID.randomUUID().toString().take(8)
        val user = userRepository.save(
            User(
                id = null,
                userId = userId,
                email = "e2e-$unique@example.com",
                emailVerified = true,
                displayName = "E2E",
                createdAt = OffsetDateTime.now(),
                lastLoginAt = null
            )
        ).block()!!

        val userJwt = jwtService.issueAccessToken(user)

        // --- 1. Create a trackable via the API (owner taken from the JWT principal) ---
        val createdTrackable = webTestClient.post()
            .uri("/api/trackables")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $userJwt")
            .bodyValue(CreateTrackableRequest(name = "E2E trackable", title = "E2E", hostName = "host-$unique"))
            .exchange()
            .expectStatus().isOk
            .expectBody(Map::class.java)
            .returnResult().responseBody!!
        val trackableId = createdTrackable["trackableId"] as String
        assertThat(trackableId).isNotBlank()

        // --- 2. Issue a device token (USER auth, must own the trackable) ---
        val issued = webTestClient.post()
            .uri("/api/trackables/$trackableId/devices/token")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $userJwt")
            .exchange()
            .expectStatus().isOk
            .expectBody(QuickCreateRes::class.java)
            .returnResult().responseBody!!
        val deviceToken = issued.deviceToken
        assertThat(deviceToken).isNotBlank()

        // --- 3. Register a device with that token (public endpoint) ---
        val deviceId = "device-$unique"
        webTestClient.post()
            .uri("/api/trackables/$trackableId/devices/register")
            .bodyValue(RegisterDevice(deviceId = deviceId, name = "E2E Device", token = deviceToken))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.deviceId").isEqualTo(deviceId)
            .jsonPath("$.trackableId").isEqualTo(trackableId)

        // --- 4. Submit multiple positions (device auth via X-Device-Token) ---
        val base = Instant.parse("2026-01-01T12:00:00Z")
        val positions = listOf(
            PositionRequest(latitude = 52.0, longitude = 4.0, createdAt = base),
            PositionRequest(latitude = 52.1, longitude = 4.1, createdAt = base.plusSeconds(60)),
            PositionRequest(latitude = 52.2, longitude = 4.2, createdAt = base.plusSeconds(120)),
        )
        webTestClient.post()
            .uri("/api/trackables/$trackableId/$deviceId/positions")
            .header("X-Device-Token", deviceToken)
            .bodyValue(positions)
            .exchange()
            .expectStatus().isNoContent

        // --- 5. Create a trip whose window brackets the positions (positionType ALL = exact rows) ---
        val slug = "summer-trip"
        webTestClient.post()
            .uri("/api/trackables/$trackableId/trips")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $userJwt")
            .bodyValue(
                CreateTripRequest(
                    title = "Summer Trip",
                    slug = slug,
                    startDate = base.minusSeconds(3600),
                    endDate = base.plusSeconds(3600),
                    positionType = PositionType.ALL,
                )
            )
            .exchange()
            .expectStatus().isNoContent

        // --- 6. Read the positions back through the public trip endpoint and validate them ---
        val returned = webTestClient.get()
            .uri("/api/trackables/$trackableId/trips/$slug/positions")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(PositionReport::class.java)
            .returnResult().responseBody!!.filterNotNull()

        assertThat(returned).hasSize(3)
        // Endpoint orders newest-first
        assertThat(returned.map { it.createdAt }).containsExactly(
            base.plusSeconds(120), base.plusSeconds(60), base
        )
        assertThat(returned.map { Triple(it.latitude, it.longitude, it.createdAt) })
            .containsExactlyInAnyOrder(
                Triple(52.0, 4.0, base),
                Triple(52.1, 4.1, base.plusSeconds(60)),
                Triple(52.2, 4.2, base.plusSeconds(120)),
            )
        assertThat(returned).allSatisfy { assertThat(it.trackableId).isEqualTo(trackableId) }
    }
}
