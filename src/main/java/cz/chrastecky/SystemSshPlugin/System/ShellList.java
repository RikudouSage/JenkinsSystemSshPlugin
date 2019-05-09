package cz.chrastecky.SystemSshPlugin.System;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShellList {

    private List<String> shells = null;

    @Nonnull
    public List<String> getShells() {
        if (shells == null) {
            shells = new ArrayList<>();

            try {
                BufferedReader reader = new BufferedReader(new FileReader("/etc/shells"));
                String line;
                while((line = reader.readLine()) != null) {
                    if(line.trim().indexOf("#") == 0) {
                        continue;
                    }
                    File shell = new File(line.trim());
                    if(!shell.exists()) {
                        continue;
                    }
                    shells.add(shell.getAbsolutePath());
                }
                reader.close();
            } catch (IOException e) {
                // do nothing
            }
        }

        return shells;
    }

}
