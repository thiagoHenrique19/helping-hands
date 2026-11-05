package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.application.mapper.OrganizationMapper;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;

    private OrganizationService organizationService;
    private final OrganizationRequest request =
            new OrganizationRequest("Hope NGO", "Hope@ngo.org", "Food aid", "1199", "https://hope.org");

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(organizationRepository, new OrganizationMapper(), userRepository);
    }

    @Test
    void shouldRegisterOrganization() {
        when(organizationRepository.existsByEmail("hope@ngo.org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

        OrganizationResponse response = organizationService.register(request);

        assertThat(response.email()).isEqualTo("hope@ngo.org");
        assertThat(response.verified()).isFalse();
        assertThat(response.phone()).isEqualTo("1199");
    }

    @Test
    void shouldLinkNewOrganizationToItsOwner() {
        User owner = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(organizationRepository.existsByEmail("hope@ngo.org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> {
            Organization saved = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });

        organizationService.register(request, 7L);

        assertThat(owner.getOrganizationId()).isEqualTo(42L);
        verify(userRepository).save(owner);
    }

    @Test
    void shouldNotRegisterSecondOrganizationForSameOwner() {
        User owner = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        owner.assignOrganization(1L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(organizationRepository.existsByEmail("hope@ngo.org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> organizationService.register(request, 7L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldFailWhenOwnerDoesNotExist() {
        when(organizationRepository.existsByEmail("hope@ngo.org")).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.register(request, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateEmailOnRegister() {
        when(organizationRepository.existsByEmail("hope@ngo.org")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.register(request)).isInstanceOf(ConflictException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOrganization() {
        Organization existing = Organization.register("Old", "old@ngo.org", "d");
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(organizationRepository.existsByEmailAndIdNot("hope@ngo.org", 1L)).thenReturn(false);
        when(organizationRepository.save(existing)).thenReturn(existing);

        OrganizationResponse response = organizationService.update(1L, request);

        assertThat(response.name()).isEqualTo("Hope NGO");
        assertThat(response.website()).isEqualTo("https://hope.org");
    }

    @Test
    void shouldRejectUpdateWithEmailOfAnotherOrganization() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(Organization.register("Old", "old@ngo.org", "d")));
        when(organizationRepository.existsByEmailAndIdNot("hope@ngo.org", 1L)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.update(1L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldFailUpdatingUnknownOrganization() {
        when(organizationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.update(9L, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldVerifyOrganization() {
        Organization existing = Organization.register("Old", "old@ngo.org", "d");
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(organizationRepository.save(existing)).thenReturn(existing);

        assertThat(organizationService.verify(1L).verified()).isTrue();
    }

    @Test
    void shouldNotVerifyTwice() {
        Organization existing = Organization.register("Old", "old@ngo.org", "d");
        existing.verify();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> organizationService.verify(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldSearchOrganizationsByName() {
        PageQuery query = PageQuery.of(0, 10);
        when(organizationRepository.searchByName("hope", query))
                .thenReturn(new PageResult<>(List.of(Organization.register("Hope NGO", "h@ngo.org", "d")), 0, 10, 1, 1));

        PageResult<OrganizationResponse> result = organizationService.search("hope", query);

        assertThat(result.content()).extracting(OrganizationResponse::name).containsExactly("Hope NGO");
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
