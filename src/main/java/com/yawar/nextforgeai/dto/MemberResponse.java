package com.yawar.nextforgeai.dto;

import com.yawar.nextforgeai.entity.ProjectMember;
import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberResponse implements Serializable {
    private String id;
    private String username;
    private String email;
    private String name;
    private ProjectMemberRole projectMemberRole;
    private Instant createdAt;
}
