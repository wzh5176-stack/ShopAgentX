package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.controller.KnowledgeController;
import com.wwzyhy.shopagent.dto.*;
import com.wwzyhy.shopagent.entity.Product;
import com.wwzyhy.shopagent.mapper.ProductMapper;
import com.wwzyhy.shopagent.mapper.ProductInventoryMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KnowledgeScopeTest {
    @Test void selectedIdentityComesFromActualProduct() {
        var products = mock(ProductService.class);
        var ai = mock(AiClient.class);
        var p = new Product(); p.setId(42L); p.setTitle("澄声 E02 降噪蓝牙耳机"); p.setBrand("澄声演示");
        when(products.findOnSaleById(42L)).thenReturn(p);
        var reply = new AiKnowledgeData("资料不足", false, List.of());
        when(ai.askKnowledge("怎么充电", p.getTitle(), p.getBrand())).thenReturn(reply);
        var result = new KnowledgeController(ai, products).askKnowledge(new KnowledgeRequest(" 怎么充电 ", 42L));
        assertSame(reply, result.get("data"));
        verify(ai).askKnowledge("怎么充电", p.getTitle(), p.getBrand());
    }

    @Test void hiddenOrMissingProductNeverReachesAi() {
        var products = mock(ProductService.class);
        var ai = mock(AiClient.class);
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeController(ai, products)
                .askKnowledge(new KnowledgeRequest("怎么充电", 999L)));
        verifyNoInteractions(ai);
    }

    @Test void legacyQuestionOnlyStillWorks() {
        var products = mock(ProductService.class);
        var ai = mock(AiClient.class);
        when(ai.askKnowledge("K1怎么充电")).thenReturn(new AiKnowledgeData("资料不足", false, List.of()));
        new KnowledgeController(ai, products).askKnowledge(new KnowledgeRequest("K1怎么充电", null));
        verify(ai).askKnowledge("K1怎么充电");
        verifyNoInteractions(products);
    }

    private String answer(String document) {
        return """
            {"data":{"answer":"支持无线充电，演示资料","answerable":true,"sources":[{
              "chunkId":"demo-audio-e02:2","source":"catalog/demo-audio-e02.md",
              "document":"%s","section":"充电与续航","content":"支持无线充电。"}]}}
            """.formatted(document);
    }

    @Test void selectedIdentityIsSentAndMatchingVersionedCitationAccepted() {
        var builder = RestClient.builder().baseUrl("http://ai.invalid");
        var server = MockRestServiceServer.bindTo(builder).build();
        var ai = new AiClient(builder.build());
        server.expect(requestTo("http://ai.invalid/api/knowledge/ask"))
                .andExpect(content().json("""
                    {"question":"怎么充电","productTitle":"澄声 E02","productBrand":"澄声演示"}
                    """))
                .andRespond(withSuccess(answer("澄声 E02 · 资料版本 2026-10-25.1 · 虚构演示"), MediaType.APPLICATION_JSON));
        assertTrue(ai.askKnowledge("怎么充电", "澄声 E02", "澄声演示").answerable());
        server.verify();
    }

    @Test void anotherModelCitationIsRejectedByJava() {
        var builder = RestClient.builder().baseUrl("http://ai.invalid");
        var server = MockRestServiceServer.bindTo(builder).build();
        var ai = new AiClient(builder.build());
        server.expect(anything()).andRespond(withSuccess(answer("澄声 E01 · 资料版本 2026-10-25.1 · 虚构演示"), MediaType.APPLICATION_JSON));
        assertThrows(RestClientException.class, () -> ai.askKnowledge("怎么充电", "澄声 E02", "澄声演示"));
        server.verify();
    }

    private ProductCreateRequest request(String image) {
        return new ProductCreateRequest("演示商品", "演示", new BigDecimal("19.90"), image, "演示", 1);
    }

    @Test void localCatalogIllustrationCanBeUsedInProductEdits() {
        var mapper = mock(ProductMapper.class);
        var inventory = mock(ProductInventoryMapper.class);
        when(mapper.insert(any(Product.class))).thenAnswer(invocation -> { ((Product) invocation.getArgument(0)).setId(1L); return 1; });
        when(inventory.insert(any(com.wwzyhy.shopagent.entity.ProductInventory.class))).thenReturn(1);
        var result = new ProductService(mapper, inventory).create(request("/shopagent/catalog/e02.svg"));
        assertEquals("/shopagent/catalog/e02.svg", result.getImageUrl());
    }

    @Test void illustrationWhitelistRejectsTraversalAndExecutableSchemes() {
        var mapper = mock(ProductMapper.class);
        var inventory = mock(ProductInventoryMapper.class);
        var service = new ProductService(mapper, inventory);
        for (String bad : List.of("/shopagent/catalog/../e02.svg", "//evil.invalid/image.svg",
                "javascript:alert(1)", "/shopagent/catalog/e02.svg?file=secret", "file:///secrets/key")) {
            assertThrows(IllegalArgumentException.class, () -> service.create(request(bad)), bad);
        }
        verifyNoInteractions(mapper, inventory);
    }
}
