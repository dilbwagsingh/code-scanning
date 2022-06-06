package com.mykaarma.kcommunications_model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@Data
public class InternalCommentAttributes implements Serializable {

    @ApiModelProperty(notes = "users which are tagged on internal comment")
    protected List<User> usersToNotify;

    @ApiModelProperty(notes = "users which are followers to customer thread")
    protected List<User> usersFollowing;
    
    @ApiModelProperty(notes = "group members involved if a group is tagged")
    protected HashMap<String, List<User>> groupMembers;

}
