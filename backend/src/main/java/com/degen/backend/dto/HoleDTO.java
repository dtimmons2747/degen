package com.degen.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoleDTO {
    private Long id;
    private Integer holeNumber;
    private Integer par;
    private Integer yards;
    private Integer handicap;
    private Long courseId;
}
