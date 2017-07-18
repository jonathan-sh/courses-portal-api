package com.courses.portal.security.controller;


import com.courses.portal.security.AppConstant;
import com.courses.portal.security.TokenUtils;
import com.courses.portal.security.model.Login;
import com.courses.portal.security.model.AuthenticationResponse;
import com.courses.portal.security.model.SpringSecurityUser;
import com.courses.portal.security.service.UserDetailsServiceImpl;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/login")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> authenticationRequest(@RequestBody Login login)
            throws AuthenticationException {

        Authentication authentication =
                this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(login.getUsername());
        Document token = new Document();
        token.put("token",this.tokenUtils.generateToken(userDetails));

        return ResponseEntity.ok(token);
    }




    @RequestMapping(value = "/refresh", method = RequestMethod.GET)
    public ResponseEntity<?> authenticationRequest(HttpServletRequest request) {
        String token = request.getHeader(AppConstant.tokenHeader);
        String username = this.tokenUtils.getUsernameFromToken(token);
        SpringSecurityUser user = (SpringSecurityUser) this.userDetailsService.loadUserByUsername(username);

        if (this.tokenUtils.canTokenBeRefreshed(token, user.getLastPasswordReset()))
        {
            String refreshedToken = this.tokenUtils.refreshToken(token);
            return ResponseEntity.ok(new AuthenticationResponse(refreshedToken));
        }
        else
        {
            return ResponseEntity.badRequest().body(null);
        }
    }

}
