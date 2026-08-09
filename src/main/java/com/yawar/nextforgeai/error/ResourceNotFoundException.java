package com.yawar.nextforgeai.error;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@Getter
public class ResourceNotFoundException extends RuntimeException {
  String resourceName;
  String resourceId;
}