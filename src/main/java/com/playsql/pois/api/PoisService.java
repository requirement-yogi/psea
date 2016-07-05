package com.playsql.pois.api;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PoisService {
    File export(Consumer<WorkbookAPI> f);
}
