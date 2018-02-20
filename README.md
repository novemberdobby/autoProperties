# AutoProperties - TeamCity plugin to set parameters based on how a build was triggered
AutoProperties provides a build feature that can set parameters based on whether or not the build was triggered by a person. It can also set different parameters depending on those that exist already. For example, you might want to change behaviour based on the amount of system memory available to an agent (*teamcity.agent.hardware.memorySizeMb*).

The plugin will run before any build steps are executed, so updated parameters will be immediately available.

AutoProperties came about as a partial solution to [TW-6439 - Set custom parameters on automatic build triggering](https://youtrack.jetbrains.com/issue/TW-6439).

## Usage
Add the feature to any build config or template:
![auto](/images/auto.png)
![manual](/images/manual.png)
Any number of parameters can be given. Spaces are trimmed from both the name and value.

#### Missing parameters
As you type, the list will be tested for any target parameters that don't exist:
![missing_vars](/images/missing_vars.png)

#### By trigger type
In this mode, the type of trigger that initiated the build is tested. For example, to set parameters only in builds started by VCS triggers, use 'vcs':
![by_trigger_type](/images/by_trigger_type.png)

See "Testing" to find out the trigger type name for previous builds (previous names will also be listed when typing in this field).
Note: the server doesn't currently provide any way to determine exactly which trigger was responsible.

#### Custom
In this mode, a list of available parameters will appear (this is aggregated from the build config as well as the last completed build, if there is one):
![custom_params](/images/custom_params.png)

The regular expression to match against will be checked on save/test:
![custom_pattern_error](/images/custom_pattern_error.png)

#### Testing
Click "Test on previous builds" to check which builds would qualify to have parameters set (this example is the character '3' in %build.number%):
![test_previous](/images/test_previous.png)

For the 'By trigger type' setting, the trigger type name is listed in the rightmost column.

## Build chains / Dependencies
When builds are triggered indirectly as part of a dependency chain, they are treated as being triggered in the same way that the "responsible" build was. Use the test functionality to be sure of what will happen in these cases.

Warning: builds that are modified by AutoProperties are not considered to have customised parameters, unless the user specifies some in the Run dialog. This may have unintentional effects on build re-use, see ["Suitable Builds"](https://confluence.jetbrains.com/display/TCD10/Snapshot+Dependencies) in the TeamCity documentation.


## Building
This plugin is built with Maven. To compile it, run the following in the root folder:

```
mvn package
```

## TODO
- Add a timeout to the custom variable regex matcher to prevent catastrophic backtracking
- Investigate setting parameters on specific trigger (this isn't currently exposed in the API)