package by.ares.transaction_validation_service;

import by.ares.transaction_validation_service.dto.TwelveDataResponseDto;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static by.ares.transaction_validation_service.TestConstants.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static WireMockServer wireMockServer =
            new WireMockServer(WireMockConfiguration.wireMockConfig().port(WIREMOCK_PORT));

    protected static ObjectMapper objectMapper;

    @ServiceConnection
    public static final PostgreSQLContainer postgresContainer =
            new PostgreSQLContainer(DockerImageName.parse(DOCKER_IMAGE_POSTGRES));

    @ServiceConnection
    public static final RedisContainer redisContainer =
            new RedisContainer(DockerImageName.parse("redis:8-alpine"))
                    .withExposedPorts(6379);

    @BeforeAll
    static void init() {
        postgresContainer.start();
        redisContainer.start();
        wireMockServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @BeforeEach
    void setUpStubs() throws Exception {
        wireMockServer.resetAll();
        stubTwelveDataRates();
    }

    @AfterAll
    static void stopServices() {
        wireMockServer.stop();
        redisContainer.stop();
        postgresContainer.stop();
    }

    protected void stubTwelveDataRates() throws Exception {
        List<TwelveDataResponseDto.ValueDto> values = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            String date = String.format(DATE_TEMPLATE_2022_JAN, i);
            values.add(new TwelveDataResponseDto.ValueDto(date, RATE_500_00_STR, RATE_500_00_STR, RATE_500_00_STR,
                    RATE_500_00_STR));
        }
        var mockResponse = new TwelveDataResponseDto(new TwelveDataResponseDto.MetaDto(PAIR_USD_KZT, INTERVAL_1DAY),
                values, STATUS_OK);
        wireMockServer.stubFor(get(urlPathEqualTo(PATH_TIME_SERIES))
                .withQueryParam(QUERY_PARAM_SYMBOL, equalTo(PAIR_USD_KZT))
                .willReturn(aResponse()
                        .withStatus(HTTP_STATUS_OK)
                        .withHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(mockResponse))));
    }
}