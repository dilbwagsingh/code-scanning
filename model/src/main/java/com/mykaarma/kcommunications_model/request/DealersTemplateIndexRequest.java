package com.mykaarma.kcommunications_model.request;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DealersTemplateIndexRequest {

	List<String> dealerUuids=new ArrayList<String>();
}
