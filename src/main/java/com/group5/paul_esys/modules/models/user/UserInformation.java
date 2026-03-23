package com.group5.paul_esys.modules.models.user;

import lombok.Data;

@Data
public class UserInformation {
  private Integer userId;
  private String email;
  private String password;
  private Role role;
}
