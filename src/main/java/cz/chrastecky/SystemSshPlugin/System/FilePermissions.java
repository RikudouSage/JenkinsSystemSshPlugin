package cz.chrastecky.SystemSshPlugin.System;

import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class FilePermissions extends HashSet<PosixFilePermission> {

    public FilePermissions(PosixFilePermission... permissions) {
        super(new ArrayList<>(Arrays.asList(permissions)));
    }

}
