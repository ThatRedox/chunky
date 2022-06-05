/* Copyright (c) 2022 Chunky contributors
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.llbit.chunky.launcher;

import se.llbit.util.OSDetector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavaFxInstaller {

  private static final String HELP_LINK = "https://chunky.lemaik.de/java11";

  private final OSDetector.OS os = OSDetector.getOS();
  private final String arch = System.getProperty("os.arch").toLowerCase();
  private final Path target;
  private URL updateSite;
  private URL fxDownload;

  private JFrame window = null;

  private static class InstallationException extends Exception {
    public InstallationException(String message) {
      super(message);
    }
  }

  private JavaFxInstaller(LauncherSettings settings) throws InstallationException {
    // Get installation target directory
    Path chunkyDir = Paths.get(System.getProperty("user.home"));
    chunkyDir = chunkyDir.resolve(".chunky");
    target = chunkyDir.resolve("javafx");
    if (target.toFile().exists()) {
      throw new InstallationException("Installation already exists.");
    }

    // Get update site
    try {
      if (settings == null) {
        updateSite = new URL(LauncherSettings.DEFAULT_UPDATE_SITE);
      } else {
        updateSite = new URL(settings.updateSite);
      }
    } catch (MalformedURLException e) {
      throw new InstallationException("Invalid update site: " + e);
    }

    findFxDownload();

    showInstallDialog();
  }

  public static void launch(LauncherSettings settings, String[] args) {
    try {
      JavaFxInstaller instance = new JavaFxInstaller(settings);

      // Wait until we are finished
      try {
        synchronized (instance) {
          while (!instance.isFinished()) {
            // Have a timeout in case the window closes but we are not notifed
            // for some reason
            instance.wait(10000);
          }
        }
      } catch (InterruptedException ignored) {}

      // Success?
      JavaFxLocator.retryWithJavafx(args);
    } catch (InstallationException e) {
      showJavafxError(e);
    }
  }

  private void downloadAndInstall() throws InstallationException {
    // Zip extraction code from
    // https://mkyong.com/java/how-to-decompress-files-from-a-zip-file/
    try (ZipInputStream zis = new ZipInputStream(fxDownload.openStream())) {
      ZipEntry entry = zis.getNextEntry();
      while (entry != null) {
        boolean isDirectory = entry.getName().endsWith("/") || entry.getName().endsWith("\\");

        // Protect against zip slip
        Path newPath = target.resolve(entry.getName());
        Path normalizedPath = newPath.normalize();
        if (!normalizedPath.startsWith(target)) {
          cleanupTarget();
          throw new InstallationException("Bad zip entry: " + entry.getName());
        }

        if (isDirectory) {
          Files.createDirectories(newPath);
        } else {
          if (newPath.getParent() != null) {
            Files.createDirectories(newPath.getParent());
          }
          Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
        }

        entry = zis.getNextEntry();
      }
      zis.closeEntry();
    } catch (IOException ex) {
      try {
        cleanupTarget();
      } catch (IOException ignored) {
        System.err.println("Could not clean up target directory.");
      }
      throw new InstallationException(ex.getMessage());
    }
  }

  private void cleanupTarget() throws IOException {
    Files.walk(target)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
    assert !target.toFile().exists();
  }

  private void findFxDownload() throws InstallationException {
    try {
      fxDownload = new URL("https://download2.gluonhq.com/openjfx/17.0.2/openjfx-17.0.2_windows-x64_bin-sdk.zip");
    } catch (MalformedURLException e) {
      throw new InstallationException(e.getMessage());
    }
  }

  private void showInstallDialog() throws InstallationException {
    Image chunkyImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource(
      "/se/llbit/chunky/launcher/ui/chunky-cfg.png"));

    JFrame window = new JFrame("Install JavaFX");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setIconImage(chunkyImage);

    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

    // Title
    JLabel installLabel = new JLabel("Install JavaFX");
    installLabel.setFont(new Font(installLabel.getFont().getFontName(), Font.BOLD, 36));
    textPanel.add(installLabel);

    // Description
    JLabel[] description = new JLabel[3];
    description[0] = new JLabel("Chunky needs JavaFX to function. If you are using");
    description[1] = new JLabel("a JVM for Java 11 or later, JavaFX is no longer");
    description[2] = new JLabel("shipped alongside and must be installed separately.");
    for (JLabel label : description) {
      label.setFont(new Font(label.getFont().getFontName(), Font.PLAIN, 12));
      textPanel.add(label);
    }

    // Spacer
    textPanel.add(new JLabel(" "));

    // Computer configuration
    textPanel.add(new JLabel("Detected computer configuration:"));

    JPanel compPanel = new JPanel();
    compPanel.setLayout(new GridLayout(2, 3));
    textPanel.add(compPanel);

    compPanel.add(new JLabel("    "));
    compPanel.add(new JLabel("OS:"));
    compPanel.add(new JLabel(String.valueOf(os)));

    compPanel.add(new JLabel("    "));
    compPanel.add(new JLabel("Arch:"));
    compPanel.add(new JLabel(arch));

    // Spacer
    textPanel.add(new JLabel(" "));

    // JavaFX Licensing
    JLabel licenseText = new JLabel("JavaFX is licensed under GPLv2+LE.");
    licenseText.setFont(new Font(licenseText.getFont().getFontName(), Font.PLAIN, 12));
    textPanel.add(licenseText);
    textPanel.add(getLinkLabel());

    // Spacer
    textPanel.add(new JLabel(" "));

    // Download and Install button
    JButton downloadButton = new JButton("Download and Install");
    textPanel.add(downloadButton);
    downloadButton.addActionListener(e -> {
      try {
        this.downloadAndInstall();
        window.setVisible(false);
        window.dispose();
      } catch (InstallationException ex) {
        window.setVisible(false);
        window.dispose();
        showJavafxError(ex);
      }
      synchronized (this) {
        this.notifyAll();
      }
    });

    // Spacer
    textPanel.add(new JLabel(" "));

    // Add icon and make top layout
    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
    topPanel.add(new JLabel(new ImageIcon(chunkyImage)));
    topPanel.add(textPanel);
    topPanel.add(new JLabel("  "));

    window.add(topPanel);
    window.pack();
    window.setVisible(true);
    this.window = window;
  }

  private boolean isFinished() {
    if (this.window == null) {
      return false;
    }
    return !this.window.isVisible();
  }

  private static JLabel getLinkLabel() {
    JLabel faqLabel;
    if(Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      faqLabel = new JLabel("Click here for more information.");
      Font font = faqLabel.getFont();
      Map attributes = font.getAttributes();
      attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
      faqLabel.setFont(font.deriveFont(attributes));
      faqLabel.setForeground(Color.BLUE.darker());
      faqLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      faqLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          try {
            Desktop.getDesktop().browse(new URI(HELP_LINK));
          } catch(IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
          }
        }
      });
    } else {
      faqLabel = new JLabel(String.format("For more information see: %s", HELP_LINK));
      faqLabel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }
    return faqLabel;
  }

  private static void showJavafxError(InstallationException e) {
    String[] errorMessages = new String[]{
      "Error installing JavaFX: " + e.getMessage(),
      "If you are using a JVM for Java 11 or later, " +
        "JavaFX is no longer shipped alongside and must be installed separately.",
      "If you already have JavaFX installed, you need to run Chunky with the command:",
      "java --module-path <path/to/JavaFX/lib> --add-modules javafx.controls,javafx.fxml -jar <path/to/ChunkyLauncher.jar>"
    };
    String faqLink = "https://chunky.lemaik.de/java11";
    String faqMessage = "Check out this page for more information on how to use Chunky with JavaFX";
    if(!GraphicsEnvironment.isHeadless()) {
      JTextField faqLabel;
      if(Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        faqLabel = new JTextField(faqMessage);
        Font font = faqLabel.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        faqLabel.setFont(font.deriveFont(attributes));
        faqLabel.setForeground(Color.BLUE.darker());
        faqLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        faqLabel.setEditable(false);
        faqLabel.setBackground(null);
        faqLabel.setBorder(null);
        faqLabel.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            try {
              Desktop.getDesktop().browse(new URI(faqLink));
            } catch(IOException | URISyntaxException ioException) {
              ioException.printStackTrace();
            }
          }
        });
      } else {
        faqLabel = new JTextField(String.format("%s: %s", faqMessage, faqLink));
        faqLabel.setEditable(false);
        faqLabel.setBackground(null);
        faqLabel.setBorder(null);
        faqLabel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
      }
      Object[] dialogContent = {
        Arrays.stream(errorMessages).map(msg -> {
          JTextField field = new JTextField(msg);
          field.setEditable(false);
          field.setBackground(null);
          field.setBorder(null);
          field.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
          return field;
        }).toArray(),
        faqLabel
      };
      JOptionPane.showMessageDialog(null, dialogContent, "Cannot find JavaFX", JOptionPane.ERROR_MESSAGE);
    }
    for(String message : errorMessages) {
      System.err.println(message);
    }
    System.err.printf("%s: %s\n", faqMessage, faqLink);
  }
}
