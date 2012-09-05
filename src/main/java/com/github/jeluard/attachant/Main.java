/*
 * Copyright 2012 Julien Eluard.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jeluard.attachant;

import com.google.common.base.Optional;

/**
 * Simple command line tool facilitating agent attachment to local VMs.
 */
public class Main {

  private static void ensureArguments(final String[] args, final int count) {
    if (args.length < count) {
      System.out.println("Expects at least "+count+" arguments.");
      Main.printSummary();
      System.exit(0);
    }
  }

  private static void printSummary() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Usage: java -jar attachant.jar id [options]\n");
    builder.append("\n");
    builder.append("where options include:\n");
    builder.append("\tload agentJarPath (options)\t\t\t\t\tTo load specified agent jar\n");
    builder.append("\tload-self (options)\t\t\t\t\t\tTo load this jar as an agent\n");
    builder.append("\tload-remote-management port authenticate ssl (options)\t\tTo load remote-management agent\n");
    
    System.out.println(builder.toString());
  }

  private static String extractOptions(final String[] args, final int count) {
      if (args.length > count) {
        return args[count];
      } else {
        return null;
      }
  }

  public static void main(final String[] args) throws Exception {
    Main.ensureArguments(args, 2);
    final String id = args[0];
    final String command = args[1];
    if ("load".equals(command)) {
      final int minimumArgumentCount = 3;
      Main.ensureArguments(args, minimumArgumentCount);

      final String agentJarPath = args[2];
      Agents.load(agentJarPath, id, Optional.fromNullable(Main.extractOptions(args, minimumArgumentCount)));
    } else if ("load-self".equals(command)) {
      final int minimumArgumentCount = 2;
      Main.ensureArguments(args, minimumArgumentCount);

      Agents.loadSelf(Agents.class, id, Optional.fromNullable(Main.extractOptions(args, minimumArgumentCount)));
    } else if ("load-remote-management".equals(command)) {
      final int minimumArgumentCount = 5;
      Main.ensureArguments(args, minimumArgumentCount);

      final int port = Integer.valueOf(args[2]);
      final boolean authenticate = Boolean.valueOf(args[3]);
      final boolean ssl = Boolean.valueOf(args[4]);
      Agents.loadRemoteManagement(id, port, authenticate, ssl, Optional.fromNullable(Main.extractOptions(args, minimumArgumentCount)));
    } else {
      throw new IllegalArgumentException("Unknown command <"+command+">");
    }
  }

}
