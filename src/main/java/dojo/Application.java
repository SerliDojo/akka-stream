package dojo;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Application {
    private static final Pattern metadataPattern = Pattern.compile("(?<date>\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s(?<type>\\w+)\\s.*");

    public static void main(String[] args) throws URISyntaxException {
        String name = "itc_FUN_01.log";

        Flow<String, String, NotUsed> errorsOnly = Flow.<String>create()
                .filter(line -> line.contains("ERROR"));

        Flow<String, Optional<Map<String, String>>, NotUsed> metadataExtractor = Flow.<String>create()
                .map(Application::parse);

        Flow<Map<String, String>, String, NotUsed> metadataInliner = Flow.<Map<String, String>>create()
                .map(Map::entrySet)
                .map(Set::stream)
                .map(stream -> stream
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining(", ")));

        ActorSystem system = ActorSystem.create();

        FileIO.fromPath(Paths.get(Application.class.getClassLoader().getResource(name).toURI()))
                .via(Framing.delimiter(ByteString.fromString("\n"), 10000))
                .map(ByteString::utf8String)

                //.via(errorsOnly)
                .via(metadataExtractor)
                .mapConcat(Application::removeIfAbsent)
                .via(metadataInliner)
                .runWith(Sink.foreach(System.err::println), ActorMaterializer.create(system))
                .whenComplete((ok, ko) -> system.terminate());
    }

    private static Iterable<Map<String, String>> removeIfAbsent(Optional<Map<String, String>> entry) {
        return entry.map(Arrays::asList).orElseGet(Collections::emptyList);
    }

    private static Optional<Map<String, String>> parse(String line) {
        return Optional.ofNullable(line)
                .map(metadataPattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("date", matcher.matches() ? matcher.group("date") : "Ta mère");
                    result.put("type", matcher.matches() ? matcher.group("type") : "Ton père");
                    return result;
                });
    }

}
