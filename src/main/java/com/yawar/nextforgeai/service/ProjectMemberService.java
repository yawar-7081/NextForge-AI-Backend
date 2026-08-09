package com.yawar.nextforgeai.service;


import com.yawar.nextforgeai.dto.AddMemberRequest;
import com.yawar.nextforgeai.dto.MemberResponse;
import com.yawar.nextforgeai.dto.RemoveProjectMemberRequest;
import com.yawar.nextforgeai.dto.UpdateProjectMemberRoleRequest;
import com.yawar.nextforgeai.entity.enums.ProjectMemberRole;

import java.util.List;

public interface ProjectMemberService {
    MemberResponse addMember(String projectId, AddMemberRequest request);

    List<MemberResponse> getProjectMember(String projectId);

    MemberResponse updateMemberRole(String projectId, UpdateProjectMemberRoleRequest request);

    void removeMember(String projectId, RemoveProjectMemberRequest request);
}
