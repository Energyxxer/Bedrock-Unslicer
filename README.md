# Bedrock-Unslicer

Bedrock-Unslicer is a GUI application that will create full behavior and resource packs by merging the slices in a Minecraft Windows 10 Edition .appx.

Supports merging of json files (e.g. blocks.json, terrain_texture.json), as well as including files from the definitions folder, which aren't inside any of the versioned slices.


## Interface

The interface consists of 3 file inputs.

1. The first takes in the .appx file used to install Minecraft builds on Windows 10 Edition.
2. The second takes in a configuration json file that determines which slices to take and how to merge them.
3. The third takes in a path to a folder into which the merged resource packs will be dumped. Inside, one folder will be created for each version that has `"export_this": true` in the configuration file. Each of those will contain both the behavior pack and the resource pack for that version.

Once you set all three file paths, you can press Start to start merging.

## Input Configuration

The configration json file (second file input) is crucial to unslicing the vanilla packs. It determines which pack slices to be used for which versions, in which order they should be merged, and how to resolve collisions.

### `versions`
An array of objects. Each of these objects represents a major version.
The program scans each of these versions, merging each with the previous.

Properties:
* `layers`: An array of strings. Determines which pack names should be taken from the appx to create this version's slice.
e.g. `vanilla_1.15` refers to the 1.15 pack inside the appx.

    You may have multiple names in case a version consists of multiple packs. For example, the 1.13 resource pack slice is made up of `vanilla_base`, `vanilla` and `vanilla_music`.
    
    Note that this list of names applies for both the behavior pack and resource pack, though if a pack with a given name isn't found, it is ignored.
* `export_this`: A boolean. If true, after this version's packs have been merged with the previous, its contents will be dumped into the output folder, containing all the files from past versions up to this version.
* `name`: A string. The name of the folder this version should be exported into, if at all.
* `copy_definitions`: An array of objects. If present, will include files from the definitions folder of the appx into the specified pack of this version. e.g. `attachables`, which are useful parts of a resource pack but are in definitions, instead of a slice's resource pack.
    * `name`: A string. The name of the folder inside definitions that will be copied into the pack.
    * `into`: A string, "RP" or "BP". Determines whether this definition folder will be copied into the resource pack or the behavior pack.

### `merge_files`
An array of either strings or objects.

Each of these entries represents a file path, from the root of the resource pack or the behavior pack, whose contents are to be merged. e.g. blocks.json, terrain_texture.json.
If there is a collision between files in different slices, and its file path can't be found in this list, then the later version will fully overwrite the previous version.
Use this array to prevent this.

If the entry is an object, it can have the following properties:
* `path`: A string. The path, from the root of the resource pack or behavior pack, to the file that should be merged.
* `overwrite_keys`. An array of strings.

    By default, the program will merge any JSON objects and arrays whose keys collide. However, you may want to override this behavior for certain keys - for example, "format_version", which is *always* a 3-number array representing the version. Merging two format_version arrays into a single 6-number array is not desirable.
    Add the problematic key (e.g. "format_version") to this array to prevent any arrays or objects under that key from being merged. Instead, the later versions will overwrite the previous' values.
    
    
For your convenience, there is an **example_config.json** file in the root of the repository which should have most, if not all the settings as you would want them. It has all the versions as of the 1.16.200 beta with the Caves and Cliffs (experimental) slice, and exports both behavior and resource packs for each version between R13 and Caves and Cliffs. You can use it as a base, and change any values you may want.
