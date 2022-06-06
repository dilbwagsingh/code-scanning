package com.mykaarma.kcommunications_model.response;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetDepartmentsUsingKaarmaTwilioURLResponse extends Response {

    private Map<Long, List<Long>> dealerIDdepartmentIDListMap;
}
