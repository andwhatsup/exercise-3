// moisture_detector agent

// Add your graph repo location below
repo_location("http://localhost:7200/repositories/andreas-baumann").

!start.

+!start : repo_location(KGRepo) <- 
    .print("Hello world");
    !setup_artifacts(KGRepo);
    !manage_farm.

+!setup_artifacts(KGRepo) : true <-
    makeArtifact("farmKG1", "farm.FarmKG", [KGRepo], FarmKGId);

    queryFarm(Farm);
    +farm(Farm);
    .print("Queried farm to manage: ", Farm);

    queryThing(Farm, "https://was-course.interactions.ics.unisg.ch/farm-ontology#ReadSoilMoistureAffordance", ThingDescription);
    .print("Queried td:Thing for reading moisture: ", ThingDescription);
    makeArtifact("tractor1", "org.hyperagents.jacamo.artifacts.wot.ThingArtifact", [ThingDescription], TractorId).

+!manage_farm : farm(Farm) <-
    .print("Managing farm: ", Farm);
    queryFarmSections(Farm, Sections);
    .print("Queried sections of farm: ", Sections);
    !monitor_section(Sections).

+!manage_farm : true <-
    .print("No farm to manage").

+!monitor_section([]) : true <-
    .print("All sections have been monitored.").

+!monitor_section([Section | RemainingSections]) : true <-
    .print("Monitoring section: ", Section);

    querySectionCoordinates(Section, Coordinates);
    .print("Queried coordinates [X1,Y1,X2,Y2] of section: ", Coordinates);

    invokeAction("https://was-course.interactions.ics.unisg.ch/farm-ontology#ReadSoilMoistureAffordance", [], Coordinates, CurrentMoistureLevel);
    .print("Read moisture in section: ", CurrentMoistureLevel);

    queryCropOfSection(Section, Crop);

    .print("Queried crop in section: ", Crop);
    
    queryRequiredMoisture(Crop, RequiredMoistureLevel);
    .print("Queried required moisture level of crop: ", RequiredMoistureLevel);

    !check_moisture_sufficiency(Coordinates, RequireMoistureLevel, CurrentMoistureLevel);

    !monitor_section(RemainingSections).

+!check_moisture_sufficiency(Coordinates, RequireLevel, CurrentLevel) : RequiredLevel > CurrentLevel <-
    .print("Detected low moisture in: ", Coordinates, ". Informing irrigator.");
    .send(irrigator, tell, low_moisture(Coordinates)).

+!check_moisture_sufficiency(Coordinates, RequireLevel, CurrentLevel) : true <-
    .print("Detected sufficient moisture in: ", Coordinates).
