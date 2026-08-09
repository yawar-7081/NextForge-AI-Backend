package com.yawar.nextforgeai.controller;

import com.yawar.nextforgeai.dto.AddMemberRequest;
import com.yawar.nextforgeai.dto.MemberResponse;
import com.yawar.nextforgeai.dto.RemoveProjectMemberRequest;
import com.yawar.nextforgeai.dto.UpdateProjectMemberRoleRequest;
import com.yawar.nextforgeai.service.ProjectMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/project-member")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class ProjectMemberController {

    ProjectMemberService projectMemberService;

    @PostMapping(value = "/addMember/{projectId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable(required = true) String projectId,
            @RequestBody(required = true)AddMemberRequest request
            ){
        MemberResponse response = projectMemberService.addMember(projectId,request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{projectId}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MemberResponse>> getProjectMembers(
            @PathVariable(required = true) String projectId
    ){
        List<MemberResponse> responses = projectMemberService.getProjectMember(projectId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping(
            value = "/{projectId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MemberResponse> updateProjectMemberRole(
            @PathVariable(required = true) String projectId,
            @RequestBody(required = true) UpdateProjectMemberRoleRequest request
            ){
        MemberResponse responses = projectMemberService.updateMemberRole(projectId,request);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping(
            value = "/{projectId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> removeProjectMember(
            @PathVariable(required = true) String projectId,
            @RequestBody(required = true) RemoveProjectMemberRequest request
    ){
        projectMemberService.removeMember(projectId,request);
        return ResponseEntity.ok().build();
    }

}
