package com.degen.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlayerSelectionInfo {
  private Long id;
  private String name;
  private Boolean partTime;
}
