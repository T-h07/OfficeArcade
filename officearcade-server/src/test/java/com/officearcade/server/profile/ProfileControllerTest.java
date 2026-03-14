package com.officearcade.server.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.store.dto.StoreCatalogResponse;
import com.officearcade.server.store.dto.StorePurchaseResponse;
import java.util.Comparator;
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
class ProfileControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerProfileEntityRepository playerProfileEntityRepository;

    @Test
    void shouldReturnProfileAggregateAndLayeredAvatarData() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        TestUser user = createEmployeeUser(adminToken, "profile-loadout");
        setRespect(user.id(), 260);
        String userToken = loginAndGetToken(user.email(), user.password());

        ResponseEntity<StoreCatalogResponse> catalogResponse = restTemplate.exchange(
                baseUrl("/api/store/catalog"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                StoreCatalogResponse.class
        );
        assertThat(catalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catalogResponse.getBody()).isNotNull();

        String outfitItemId = catalogResponse.getBody().items().stream()
                .filter(item -> "OUTFIT".equals(item.category()))
                .findFirst()
                .orElseThrow()
                .id();

        String hatItemId = catalogResponse.getBody().items().stream()
                .filter(item -> "HAT".equals(item.category()))
                .findFirst()
                .orElseThrow()
                .id();

        ResponseEntity<StorePurchaseResponse> outfitPurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + outfitItemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                StorePurchaseResponse.class
        );
        assertThat(outfitPurchase.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<StorePurchaseResponse> hatPurchase = restTemplate.exchange(
                baseUrl("/api/store/purchase/" + hatItemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                StorePurchaseResponse.class
        );
        assertThat(hatPurchase.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> equipOutfit = restTemplate.exchange(
                baseUrl("/api/inventory/equip/" + outfitItemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertThat(equipOutfit.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> equipHat = restTemplate.exchange(
                baseUrl("/api/inventory/equip/" + hatItemId),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertThat(equipHat.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ProfileMeResponse> profileResponse = restTemplate.exchange(
                baseUrl("/api/profile/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                ProfileMeResponse.class
        );

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody()).isNotNull();
        ProfileMeResponse profile = profileResponse.getBody();

        assertThat(profile.userId()).isEqualTo(user.id());
        assertThat(profile.ownedCosmeticCount()).isGreaterThanOrEqualTo(2);
        assertThat(profile.equippedCosmeticCount()).isGreaterThanOrEqualTo(2);
        assertThat(profile.avatarLayers()).isNotEmpty();
        assertThat(profile.avatarLayers().get(0).category()).isEqualTo("BASE_BODY");
        assertThat(profile.equippedCosmetics().stream().map(ProfileEquippedCosmetic::category))
                .contains("OUTFIT", "HAT");

        List<ProfileEquippedCosmetic> sortedByLayerOrder = profile.equippedCosmetics().stream()
                .sorted(Comparator.comparingInt(ProfileEquippedCosmetic::layerOrder))
                .toList();
        assertThat(profile.equippedCosmetics()).containsExactlyElementsOf(sortedByLayerOrder);
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Profile@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, "Profile Test User", password, AppRole.EMPLOYEE, true),
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

    private record ProfileMeResponse(
            String userId,
            int ownedCosmeticCount,
            int equippedCosmeticCount,
            List<ProfileAvatarLayer> avatarLayers,
            List<ProfileEquippedCosmetic> equippedCosmetics
    ) {
    }

    private record ProfileAvatarLayer(String category) {
    }

    private record ProfileEquippedCosmetic(String category, int layerOrder) {
    }
}
