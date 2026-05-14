package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.ObjectTypeMetadata;
import com.prasannjeet.vaxjobostader.util.StaticUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaxjobostaderClientPaginationTest {

    @Mock HttpClient httpClient;
    @Mock @SuppressWarnings("rawtypes") HttpResponse httpResponse;

    private VaxjobostaderClient client;
    private final ObjectMapper mapper = StaticUtils.getMapper();

    @BeforeEach
    void setUp() {
        client = new VaxjobostaderClient(
            URI.create("https://example.com/api"),
            "test-key",
            httpClient,
            mapper
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllPropertiesList_singlePage_returnsAllItems() throws Exception {
        String body = """
            {"count":2,"items":[
                {"id":"a","localId":"1","displayName":"A","type":"residential","queueType":"residential","availability":{"availableFrom":"/Date(1785535200000)/"},"thumbnail":{"exists":false}},
                {"id":"b","localId":"2","displayName":"B","type":"residential","queueType":"residential","availability":{"availableFrom":"/Date(1785535200000)/"},"thumbnail":{"exists":false}}
            ]}""";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        List<HouseListItem> result = client.getAllPropertiesList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("a");
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllPropertiesList_multiPage_fetchesAllPagesAndCombines() throws Exception {
        String page1 = buildPage(50, 25, 0);
        String page2 = buildPage(50, 25, 25);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(page1, page2);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        List<HouseListItem> result = client.getAllPropertiesList();

        assertThat(result).hasSize(50);
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllPropertiesList_nonOkStatus_throwsIOException() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        assertThatThrownBy(() -> client.getAllPropertiesList())
            .isInstanceOf(IOException.class)
            .hasMessageContaining("offset=0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTypeMetadata_nonOkStatus_returnsNull() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        assertThat(client.getTypeMetadata("unknownType")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTypeMetadata_okStatus_deserializesMetadata() throws Exception {
        String body = "{\"displayName\":\"Bostad\",\"description\":\"Standard apartments\",\"numberOfMarketObjects\":169}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        ObjectTypeMetadata meta = client.getTypeMetadata("residential");

        assertThat(meta).isNotNull();
        assertThat(meta.displayName()).isEqualTo("Bostad");
        assertThat(meta.numberOfMarketObjects()).isEqualTo(169);
    }

    /** Builds a JSON list page where each item has a unique id = "item-{start+i}". */
    private static String buildPage(int total, int pageSize, int start) {
        StringBuilder sb = new StringBuilder("{\"count\":" + total + ",\"items\":[");
        for (int i = 0; i < pageSize; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"item-").append(start + i)
              .append("\",\"localId\":\"").append(start + i)
              .append("\",\"displayName\":\"Item ").append(start + i)
              .append("\",\"type\":\"residential\",\"queueType\":\"residential\"")
              .append(",\"availability\":{\"availableFrom\":\"/Date(1785535200000)/\"}")
              .append(",\"thumbnail\":{\"exists\":false}}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
