package com.skillupnow.demo.controller;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import com.auth0.jwt.JWT;
import com.skillupnow.demo.exception.SkillUpNowException;
import com.skillupnow.demo.exception.ValidationGroups;
import com.skillupnow.demo.model.UserType;
import com.skillupnow.demo.model.dto.CreateUserRequest;
import com.skillupnow.demo.model.dto.ModifyCredentialRequest;
import com.skillupnow.demo.model.dto.ModifyCustomerRequest;
import com.skillupnow.demo.model.po.Customer;
import com.skillupnow.demo.model.po.Organization;
import com.skillupnow.demo.model.po.User;
import com.skillupnow.demo.repository.UserRepository;
import com.skillupnow.demo.security.SecurityConstants;
import com.skillupnow.demo.security.UserDetailServiceImpl;
import com.skillupnow.demo.service.CustomerService;
import com.skillupnow.demo.service.OrganizationService;
import com.skillupnow.demo.service.UserService;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * The UserController class provides a RESTful API for managing users.
 * This includes creating a new user, retrieving user information, updating customer information, and updating user credentials.
 * Authentication is required for accessing some endpoints.
 * Exceptions thrown by UserService (either CustomerService or CompanyService) will be handled by ExceptionHandler.
 *
 * @author Hua Wang
 */
@RestController
public class UserController {
  @Autowired
  private CustomerService customerService;

  @Autowired
  private UserDetailServiceImpl userDetailService;

  @Autowired
  private OrganizationService organizationService;

  @Autowired
  private UserService userService;

  /**
   * Creates a new user.
   *
   * @param createUserRequest The CreateUserRequest object containing the new user's information.
   * @return A ResponseEntity containing the newly created User object and an authorization token in the response header.
   */
  @PostMapping("/signup")
  public ResponseEntity<User> createUser(@RequestBody @Validated(ValidationGroups.Insert.class) CreateUserRequest createUserRequest) {
    // Exceptions thrown by UserService (either CustomerService or CompanyService) will be handled by ExceptionHandler
    User user;
    if (createUserRequest.getUserType() == UserType.CUSTOMER) {
      user = customerService.createCustomer(createUserRequest);
    } else {
      user = organizationService.createOrganization(createUserRequest);
    }

    HttpHeaders headers = createAuthorizedHeader(userDetailService.loadUserByUsername(user.getUsername()));

    return ResponseEntity.ok().headers(headers).body(user);
  }

  /**
   * Generate token
   * @return JWT token
   */
  private HttpHeaders createAuthorizedHeader(UserDetails userDetail) {
    // Generate JWT token
    List<String> roles = userDetail.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    // add JWT token to response header
    String token = JWT.create()
        .withSubject(userDetail.getUsername())
        .withClaim("roles",  String.join(",", String.join(",", roles)))
        .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
        .sign(HMAC512(SecurityConstants.SECRET.getBytes()));
    HttpHeaders headers = new HttpHeaders();
    headers.add(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);

    return headers;
  }

  /**
   * Retrieves customer information for the current authenticated user.
   *
   * @return A ResponseEntity containing the Customer object.
   */
  @GetMapping("/customer")
  public ResponseEntity<Customer> getCustomerInfo() {
    // Get current authenticated user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = authentication.getName();
    return ResponseEntity.ok().body(customerService.findByUsername(currentUsername));
  }

  /**
   * Updates customer information for the current authenticated user.
   * @param modifyCustomerRequest The ModifyCustomerRequest object containing the updated customer information.
   * @return A ResponseEntity containing the updated ModifyCustomerRequest object.
   */
  @PutMapping("/customer")
  public ResponseEntity<ModifyCustomerRequest> updateCustomerInfo(@RequestBody @Validated(ValidationGroups.Update.class)
  ModifyCustomerRequest modifyCustomerRequest) {
    // Get current authenticated user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = authentication.getName();
    customerService.updateCustomer(modifyCustomerRequest, currentUsername);
    return ResponseEntity.ok().body(modifyCustomerRequest);
  }

  /**
   * Updates organization information for the current authenticated user.
   * @param modifyCredentialRequest The ModifyCredentialRequest object containing the updated organization information.
   * @return A ResponseEntity containing the updated ModifyCredentialRequest object.
   */
  @PutMapping("/user/credential")
  public ResponseEntity<ModifyCredentialRequest> updateCredential(@RequestBody @Validated(ValidationGroups.Update.class) ModifyCredentialRequest modifyCredentialRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = authentication.getName();
    userService.updateCredential(modifyCredentialRequest, currentUsername);
    modifyCredentialRequest.setConfirmPassword(null);
    modifyCredentialRequest.setNewPassword(null);
    return ResponseEntity.ok().body(modifyCredentialRequest);
  }
}
