package com.skillupnow.demo.controller;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

import com.auth0.jwt.JWT;
import com.skillupnow.demo.exception.ValidationGroups;
import com.skillupnow.demo.model.UserType;
import com.skillupnow.demo.model.dto.CreateUserRequest;
import com.skillupnow.demo.model.po.Organization;
import com.skillupnow.demo.model.po.User;
import com.skillupnow.demo.repository.UserRepository;
import com.skillupnow.demo.security.SecurityConstants;
import com.skillupnow.demo.security.UserDetailServiceImpl;
import com.skillupnow.demo.service.CustomerService;
import com.skillupnow.demo.service.OrganizationService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class UserController {
  @Autowired
  private CustomerService customerService;

  @Autowired
  private UserDetailServiceImpl userDetailService;

  @Autowired
  private OrganizationService organizationService;

  @PostMapping("/signup")
  public ResponseEntity<User> createUser(@RequestBody @Validated(ValidationGroups.Insert.class) CreateUserRequest createUserRequest) {
    // Exceptions thrown by UserService (either CustomerService or CompanyService) will be handled by ExceptionHandler
    User user;
    if (createUserRequest.getUserType() == UserType.CUSTOMER) {
      user = customerService.createCustomer(createUserRequest);
    } else {
      user = organizationService.createOrganization(createUserRequest);
    }

    // Generate JWT token
    org.springframework.security.core.userdetails.UserDetails userDetail = userDetailService.loadUserByUsername(user.getUsername());
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

    return ResponseEntity.ok().headers(headers).body(user);
  }

  @GetMapping("/organization/{id}")
  public ResponseEntity<Organization> getOrganizationById(@PathVariable Long id) {
    return ResponseEntity.ok().body(organizationService.findById(id));
  }
}