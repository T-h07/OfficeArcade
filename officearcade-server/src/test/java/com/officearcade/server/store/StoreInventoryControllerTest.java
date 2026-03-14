package com.officearcade.server.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.employee.dashboard.dto.EmployeeDashboardResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.store.dto.InventoryResponse;
import com.officearcade.server.store.dto.StoreCatalogResponse;
import com.officearcade.server.store.dto.StorePurchaseResponse;
import com.officearcade.server.store.dto.StoreSummaryResponse;
import com.officearcade.server.store.persistence.CosmeticItemEntity;
import com.officearcade.server.store.persistence.CosmeticItemEntityRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreInventoryControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerProfileEntityRepository playerProfileEntityRepository;

    @Autowired
    private CosmeticItemEntityRepository cosmeticItemEntityRepository;

    @Test
    void shouldPurchaseEquipSwitchAndUnequipOwnedItems() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        TestUser user = createEmployeeUser(adminToken, "store-equip");
        setRespect(user.id(), 240);
        String userToken = loginAndGetToken(user.email(), user.password());

        ResponseEntity<StoreCatalogResponse> hatCatalogResponse = restTemplate.exchange(
                baseUrl("/api/store/catalog?category=HAT"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                StoreCatalogResponse.class
        );
        assertThat(hatCatalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(hatCatalogResponse.getBody()).isNotNull();
        assertThat(hatCatalogResponse.getBody().items()).hasSizeGreaterThanOrEqualTo(2);

        String firstHatId = hatCatalogResponse.getBody().items().get(0).id();
        int firstHatPrice = hatCatalogResponse.getBody().items().get(0).priceRespect();
        String secondHatId = hatCatalogResponse.getBody().items().get(1).id();
        int secondHatPrice = hatCatalogResponse.getBody().items().get(1).priceRespect();

        ResponseEntity<StorePurchaseResponse> firstPurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + firstHatId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                StorePurchaseResponse.class
        );
        assertThat(firstPurchase.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstPurchase.getBody()).isNotNull();
        assertThat(firstPurchase.getBody().summary().respectBalance()).isEqualTo(240 - firstHatPrice);

        ResponseEntity<StorePurchaseResponse> secondPurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + secondHatId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                StorePurchaseResponse.class
        );
        assertThat(secondPurchase.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondPurchase.getBody()).isNotNull();
        int expectedRespect = 240 - firstHatPrice - secondHatPrice;
        assertThat(secondPurchase.getBody().summary().respectBalance()).isEqualTo(expectedRespect);

        ResponseEntity<InventoryResponse> equipFirst = restTemplate.exchange(
                baseUrl("/api/inventory/equip/" + firstHatId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                InventoryResponse.class
        );
        assertThat(equipFirst.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(equipFirst.getBody()).isNotNull();
        assertThat(
                equipFirst.getBody().items().stream()
                        .filter(item -> item.cosmeticItemId().equals(firstHatId))
                        .findFirst()
                        .orElseThrow()
                        .equipped()
        ).isTrue();

        ResponseEntity<InventoryResponse> equipSecond = restTemplate.exchange(
                baseUrl("/api/inventory/equip/" + secondHatId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                InventoryResponse.class
        );
        assertThat(equipSecond.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(equipSecond.getBody()).isNotNull();
        assertThat(
                equipSecond.getBody().items().stream()
                        .filter(item -> item.cosmeticItemId().equals(firstHatId))
                        .findFirst()
                        .orElseThrow()
                        .equipped()
        ).isFalse();
        assertThat(
                equipSecond.getBody().items().stream()
                        .filter(item -> item.cosmeticItemId().equals(secondHatId))
                        .findFirst()
                        .orElseThrow()
                        .equipped()
        ).isTrue();

        ResponseEntity<InventoryResponse> unequipSecond = restTemplate.exchange(
                baseUrl("/api/inventory/unequip/" + secondHatId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                InventoryResponse.class
        );
        assertThat(unequipSecond.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unequipSecond.getBody()).isNotNull();
        assertThat(
                unequipSecond.getBody().items().stream()
                        .filter(item -> item.cosmeticItemId().equals(secondHatId))
                        .findFirst()
                        .orElseThrow()
                        .equipped()
        ).isFalse();

        ResponseEntity<EmployeeDashboardResponse> dashboardResponse = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                EmployeeDashboardResponse.class
        );
        assertThat(dashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboardResponse.getBody()).isNotNull();
        assertThat(dashboardResponse.getBody().respectPoints()).isEqualTo(expectedRespect);
        assertThat(dashboardResponse.getBody().ownedCosmeticCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldBlockInvalidPurchasesAndNotOwnedEquip() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        TestUser user = createEmployeeUser(adminToken, "store-guard");
        setRespect(user.id(), 0);
        String userToken = loginAndGetToken(user.email(), user.password());

        ResponseEntity<StoreCatalogResponse> catalogResponse = restTemplate.exchange(
                baseUrl("/api/store/catalog"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                StoreCatalogResponse.class
        );
        assertThat(catalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catalogResponse.getBody()).isNotNull();
        String itemId = catalogResponse.getBody().items().stream()
                .filter(item -> item.priceRespect() > 0)
                .findFirst()
                .orElseThrow()
                .id();

        ResponseEntity<String> insufficientPurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + itemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertThat(insufficientPurchase.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> equipNotOwned = restTemplate.exchange(
                baseUrl("/api/inventory/equip/" + itemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertThat(equipNotOwned.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        setRespect(user.id(), 250);
        ResponseEntity<StorePurchaseResponse> purchaseSuccess = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + itemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                StorePurchaseResponse.class
        );
        assertThat(purchaseSuccess.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> duplicatePurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + itemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertThat(duplicatePurchase.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        CosmeticItemEntity disabledItem = new CosmeticItemEntity();
        disabledItem.setCode("DISABLED_TEST_" + UUID.randomUUID());
        disabledItem.setDisplayName("Disabled Test Item");
        disabledItem.setDescription("Disabled test item for validation.");
        disabledItem.setCategory(CosmeticCategory.BADGE);
        disabledItem.setRarity(CosmeticRarity.COMMON);
        disabledItem.setPriceRespect(5);
        disabledItem.setPreviewAssetKey("badge.disabled-test");
        disabledItem.setEnabled(false);
        CosmeticItemEntity savedDisabledItem = cosmeticItemEntityRepository.save(disabledItem);

        ResponseEntity<String> disabledItemPurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + savedDisabledItem.getId()),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertThat(disabledItemPurchase.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Store@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, "Store Test User", password, AppRole.EMPLOYEE, true),
                        authHeaders(adminToken)
                ),
                AdminUserResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        return new TestUser(createResponse.getBody().id(), email, password);
    }

    private void setRespect(String userIdText, int respect) {
        UUID userId = UUID.fromString(userIdText);
        PlayerProfileEntity profile = playerProfileEntityRepository.findByUserId(userId).orElseThrow();
        profile.setRespectPoints(Math.max(respect, 0));
        playerProfileEntityRepository.save(profile);
    }

    private String loginAndGetToken(String email, String password) {
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                baseUrl("/api/auth/login"),
                new LoginRequest(email, password),
                LoginResponse.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        return loginResponse.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private record TestUser(String id, String email, String password) {
    }
}
