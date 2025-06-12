package org.jeecg.modules.system.test;

import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;

@Data
public class SwitchDTO {
    private Integer type;
    private String event;
    private String id;
    private String name;
}
