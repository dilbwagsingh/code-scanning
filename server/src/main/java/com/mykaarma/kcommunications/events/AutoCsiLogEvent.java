package com.mykaarma.kcommunications.events;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mykaarma.kcommunications_model.request.AutoCsiLogEventRequest;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;


public class AutoCsiLogEvent implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("data")
  private List<AutoCsiLogEventRequest> data = new ArrayList<AutoCsiLogEventRequest>();

  public AutoCsiLogEvent data(List<AutoCsiLogEventRequest> data) {
    this.data = data;
    return this;
  }
  public AutoCsiLogEvent addDataItem(AutoCsiLogEventRequest dataItem) {
    this.data.add(dataItem);
    return this;
  }

  public List<AutoCsiLogEventRequest> getData() {
    return data;
  }

  public void setData(List<AutoCsiLogEventRequest> data) {
    this.data = data;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutoCsiLogEvent autocsiLogEvent = (AutoCsiLogEvent) o;
    return Objects.equals(this.data, autocsiLogEvent.data);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(data);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutocsiLogEvent {\n");
    
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
