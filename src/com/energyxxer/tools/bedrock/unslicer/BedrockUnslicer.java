package com.energyxxer.tools.bedrock.unslicer;

import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.xswing.Padding;
import com.energyxxer.xswing.XButton;
import com.energyxxer.xswing.XFileField;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class BedrockUnslicer {
    private static JsonObject configRoot;
    private static CompoundInput input;
    private static Path outDirectory = Paths.get("").toAbsolutePath().resolve("Merged");

    private static HashMap<Path, Object> packSoFar = new HashMap<>();

    private static final List<String> jsonExtensions = Arrays.asList("json", "material");
    private static final HashMap<String, MergeInstructions> mergeFiles = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static XFileField appxChooser;
    private static XFileField configChooser;
    private static XFileField outputChooser;

    private static JLabel[] infoLabels = new JLabel[3];

    private static XButton startButton;

    public static void main(String[] args) {
        JFrame window = new JFrame("Bedrock Version Merger");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                savePreferences();
            }
        });
        JPanel contentPane = new JPanel(new BorderLayout());
        window.setContentPane(contentPane);
        contentPane.setPreferredSize(new Dimension(500, 360));

        contentPane.add(new Padding(40), BorderLayout.WEST);
        contentPane.add(new Padding(40), BorderLayout.EAST);
        contentPane.add(new Padding(15), BorderLayout.NORTH);
        contentPane.add(new Padding(15), BorderLayout.SOUTH);
        JPanel centerPane = new JPanel();
        contentPane.add(centerPane, BorderLayout.CENTER);
        centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.Y_AXIS));
        centerPane.setOpaque(false);

        appxChooser = createFileField(centerPane, "appx", "The .appx file used to install the Minecraft build", XFileField.OPEN_FILE);
        configChooser = createFileField(centerPane, "Configuration File", "The .json file that determines how to merge the slices", XFileField.OPEN_FILE);
        outputChooser = createFileField(centerPane, "Output Folder", "The folder in which to place all the merged slices", XFileField.OPEN_DIRECTORY);

        JPanel infoPane = new JPanel();
        centerPane.add(infoPane);
        infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.Y_AXIS));

        for(int i = 0; i < infoLabels.length; i++) {
            JLabel label = new JLabel();
            infoLabels[i] = label;
            infoPane.add(label);
        }

        infoPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPane.setOpaque(false);

        JPanel bottomPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        centerPane.add(bottomPane);
        bottomPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPane.setOpaque(false);

        bottomPane.add(startButton = new XButton("Start"));
        startButton.addActionListener(e -> new Thread(new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() {
                try {
                    lockButton();
                    start();
                } catch (Exception x) {
                    setStatus(0, "ERROR: " + x.getMessage());
                }
                unlockButton();
                return null;
            }
        }, "Bedrock Merger Process").start());

        loadPreferences();

        window.pack();
        window.setVisible(true);
    }

    private static void setStatus(int index, String text) {
        for(int i = index+1; i < infoLabels.length; i++) {
            infoLabels[i].setText(null);
        }
        infoLabels[index].setText(text);
    }

    private static void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(BedrockUnslicer.class);
        prefs.put("input.appx", appxChooser.getFile().getAbsolutePath());
        prefs.put("input.config", configChooser.getFile().getAbsolutePath());
        prefs.put("input.output", outputChooser.getFile().getAbsolutePath());
    }

    private static void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(BedrockUnslicer.class);
        appxChooser.setFile(getSavedFile(prefs, "input.appx"));
        configChooser.setFile(getSavedFile(prefs, "input.config"));
        outputChooser.setFile(getSavedFile(prefs, "input.output"));
    }

    private static File getSavedFile(Preferences prefs, String key) {
        String savedValue = prefs.get(key, null);
        if(savedValue == null) return null;
        try {
            File file = new File(savedValue);
            if(file.exists()) return file;
            return null;
        } catch(Exception x) {
            return null;
        }
    }

    private static XFileField createFileField(JPanel container, String label, String description, byte operation) {
        JPanel wrapper = new JPanel();
        container.add(wrapper);

        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        JPanel header = new JPanel();
        wrapper.add(header);

        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        header.add(new JLabel(label + ":"));

        JLabel descComponent = new JLabel(description);
        descComponent.setFont(descComponent.getFont().deriveFont(Font.PLAIN));
        header.add(descComponent);

        XFileField field = new XFileField(operation, null);
        wrapper.add(field);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        Padding padding = new Padding(50);
        wrapper.add(padding);

        padding.setAlignmentX(Component.LEFT_ALIGNMENT);

        return field;
    }


    private static void start() throws Exception {
        setStatus(0, "Working...");

        try {
            readConfig();
        } catch(Exception x) {
            setStatus(0, "Error reading configuration: " + x.getMessage());
            return;
        }

        outDirectory = outputChooser.getFile().toPath();

        try(ZipCompoundInput _localInput = new ZipCompoundInput(appxChooser.getFile(),"data/")) {
            input = _localInput;
            input.open();

            startPackType("behavior_packs", "BP");
            startPackType("resource_packs", "RP");
        }
        setStatus(0, "All done!");
    }


    private static void lockButton() {
        startButton.setEnabled(false);
    }

    private static void unlockButton() {
        startButton.setEnabled(true);
    }

    private static void readConfig() throws IOException {
        try(FileReader fr = new FileReader(configChooser.getFile())) {
            configRoot = gson.fromJson(fr, JsonObject.class);
        }

        for(JsonElement mergeFilePathRaw : JsonTraverser.INSTANCE.reset(configRoot).get("merge_files").iterateAsArray()) {
            MergeInstructions instructions = new MergeInstructions();

            instructions.path = JsonTraverser.INSTANCE.reset(mergeFilePathRaw).asString();
            if(instructions.path == null) {
                instructions.path = JsonTraverser.INSTANCE.reset(mergeFilePathRaw).get("path").asString();
                instructions.overwriteKeys = new ArrayList<>();
                for(JsonElement overwriteKeyRaw : JsonTraverser.INSTANCE.reset(mergeFilePathRaw).get("overwrite_keys").iterateAsArray()) {
                    String overwriteKey = JsonTraverser.INSTANCE.reset(overwriteKeyRaw).asString();
                    if(overwriteKey != null) {
                        instructions.overwriteKeys.add(overwriteKey);
                    }
                }
            }

            if(instructions.path != null) {
                instructions.path = instructions.path.replace(File.separatorChar, '/');
                mergeFiles.put(instructions.path, instructions);
            }
        }
    }

    private static void startPackType(String packCollection, String outSubfileName) throws IOException {
        packSoFar.clear();

        JsonArray versionList = JsonTraverser.INSTANCE.reset(configRoot).get("versions").asJsonArray();

        for(JsonElement versionEntry : versionList) {

            String name = JsonTraverser.INSTANCE.reset(versionEntry).get("name").asString(null);

            setStatus(1, "Reading " + name + " " + outSubfileName + " slice");
            for(JsonElement layerItem : JsonTraverser.INSTANCE.reset(versionEntry).get("layers").iterateAsArray()) {
                String layerItemName = JsonTraverser.INSTANCE.reset(layerItem).asString();
                if(layerItemName != null) {
                    Path packPath = Paths.get(packCollection + "/" + layerItemName);
                    startPack(packPath);
                }
            }

            JsonArray copyDefinitions = JsonTraverser.INSTANCE.reset(versionEntry).get("copy_definitions").asJsonArray();
            if(copyDefinitions != null) {
                for(JsonElement includeDefinitionsEntry : copyDefinitions) {
                    if(includeDefinitionsEntry.isJsonObject() && outSubfileName.equals(JsonTraverser.INSTANCE.reset(includeDefinitionsEntry).get("into").asString())) {
                        String definitionName = JsonTraverser.INSTANCE.reset(includeDefinitionsEntry).get("name").asString();
                        if(definitionName != null) {
                            setStatus(1, "Reading definitions: " + definitionName);
                            startPack(Paths.get("definitions"), Paths.get("definitions", definitionName));
                        }
                    }
                }
            }

            boolean exportThis = JsonTraverser.INSTANCE.reset(versionEntry).get("export_this").asBoolean(true);

            if(exportThis) {
                setStatus(1, "Exporting " + outSubfileName + " " + name + " [" + packSoFar.size() + " entries]");
                Path outPackRoot = outDirectory.resolve(name).resolve(outSubfileName);
                outPackRoot.toFile().mkdirs();

                for(Map.Entry<Path, Object> entry : packSoFar.entrySet()) {
                    setStatus(2, "Writing " + entry.getKey());
                    File destinationFile = outPackRoot.resolve(entry.getKey()).toFile();
                    destinationFile.getParentFile().mkdirs();

                    if(entry.getValue() instanceof DirectlyFromZip) {
                        try(InputStream is = ((DirectlyFromZip) entry.getValue()).getInputStream()) {
                            Files.copy(is, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else if(entry.getValue() instanceof JsonElement) {
                        try(JsonWriter jw = new JsonWriter(new FileWriter(destinationFile))) {
                            jw.setIndent("\t");
                            gson.toJson((JsonElement) entry.getValue(), jw);
                        }
                    }
                }
            }
        }
    }

    private static void startPack(Path root) throws IOException {
        startPack(root, root);
    }

    private static void startPack(Path root, Path startReading) throws IOException {
        Queue<Path> pathsToRead = new ArrayDeque<>();
        pathsToRead.add(startReading);

        while(!pathsToRead.isEmpty()) {
            InputStream is;

            Path path = pathsToRead.remove();
            setStatus(2, "Reading path: " + path);
            if(input.isDirectory(path.toString().replace(File.separatorChar,'/'))) {
                for(String innerPath : input.listSubEntries(path.toString().replace(File.separatorChar,'/'))) {
                    pathsToRead.add(path.resolve(innerPath));
                }
            } else if((is = input.get(path.toString().replace(File.separatorChar,'/'))) != null) {
                Path relativePath = root.relativize(path);

                Object dataInFile;
                String extension = getExtension(path.getFileName().toString());
                if(jsonExtensions.contains(extension) && decideMerge(relativePath)) {
                    try(InputStreamReader fr = new InputStreamReader(is)) {
                        dataInFile = gson.fromJson(fr, JsonElement.class);
                    }
                } else {
                    //No way to merge this, simply replace the old file
                    dataInFile = new DirectlyFromZip(input, path.toString().replace(File.separatorChar,'/'), is);
                }

                //resolve collisions
                if(dataInFile instanceof JsonObject && packSoFar.get(relativePath) instanceof JsonObject && decideMerge(relativePath)) {
                    //Merging objects
                    mergeAIntoB((JsonObject) dataInFile, (JsonObject) packSoFar.get(relativePath), getMergeInstructions(relativePath));
                } else if(dataInFile instanceof JsonArray && packSoFar.get(relativePath) instanceof JsonArray && decideMerge(relativePath)) {
                    //Merging arrays
                    mergeAIntoB((JsonArray) dataInFile, (JsonArray) packSoFar.get(relativePath), getMergeInstructions(relativePath));
                } else {
                    //Replacing old file (if any)
                    packSoFar.put(relativePath, dataInFile);
                }
            }
        }
    }

    private static boolean decideMerge(Path path) {
        return mergeFiles.containsKey(path.toString().replace(File.separatorChar, '/'));
    }

    private static MergeInstructions getMergeInstructions(Path path) {
        return mergeFiles.get(path.toString().replace(File.separatorChar, '/'));
    }

    private static String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if(lastDotIndex >= 0) return filename.substring(lastDotIndex+1);
        return "";
    }

    private static void mergeAIntoB(JsonObject a, JsonObject b, MergeInstructions instructions) {
        for(Map.Entry<String, JsonElement> entry : a.entrySet()) {
            JsonElement bCounterpart = b.get(entry.getKey());

            boolean mergeForbidden = instructions.shouldOverwriteKey(entry.getKey());

            if(!mergeForbidden && entry.getValue().isJsonObject() && bCounterpart != null && bCounterpart.isJsonObject()) {
                mergeAIntoB(entry.getValue().getAsJsonObject(), bCounterpart.getAsJsonObject(), instructions);
            } else if(!mergeForbidden && entry.getValue().isJsonArray() && bCounterpart != null && bCounterpart.isJsonArray()) {
                mergeAIntoB(entry.getValue().getAsJsonArray(), bCounterpart.getAsJsonArray(), instructions);
            } else {
                b.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void mergeAIntoB(JsonArray a, JsonArray b, MergeInstructions instructions) {
        for(JsonElement element : a) {
            b.add(element);
        }
    }

    private static class MergeInstructions {
        String path;
        ArrayList<String> overwriteKeys;

        boolean shouldOverwriteKey(String key) {
            return overwriteKeys != null && overwriteKeys.contains(key);
        }
    }

    private static class DirectlyFromZip {
        private CompoundInput input;
        private String path;
        private InputStream is;

        DirectlyFromZip(CompoundInput input, String path, InputStream is) {
            this.input = input;
            this.path = path;
            this.is = is;
        }

        InputStream getInputStream() throws IOException {
            if(is != null) {
                InputStream cached = is;
                is = null;
                return cached;
            }
            return input.get(path);
        }
    }
}
