# =====================
# Compiler & flags
# =====================
CXX := g++
CXXFLAGS := -std=c++17 -O3 -Wall -Wextra
CXXFLAGS += -Isrc -Isrc/implementations
CXXFLAGS += -I/opt/homebrew/include -I/usr/local/include
LDFLAGS  := -L/opt/homebrew/lib -L/usr/local/lib
LDLIBS   := -lxxhash -lbareboneshll

# =====================
# Directories
# =====================
SRC_DIR    := src
BUILD_DIR  := build

BIN_BENCH  := $(BUILD_DIR)/benchmark_runner

# =====================
# Benchmark sources
# =====================
BENCH_SOURCES := \
	$(SRC_DIR)/UUIDFactory.cpp \
	$(SRC_DIR)/RandomFactory.cpp \
	$(SRC_DIR)/SketchTestRunner.cpp \
	$(SRC_DIR)/main.cpp

BENCH_OBJECTS := $(BENCH_SOURCES:%.cpp=$(BUILD_DIR)/%.o)

# =====================
# Default target
# =====================
all: benchmark

# =====================
# Benchmark target
# =====================
benchmark: $(BIN_BENCH)

$(BIN_BENCH): $(BENCH_OBJECTS)
	@mkdir -p $(dir $@)
	$(CXX) $^ -o $@ $(LDFLAGS) $(LDLIBS)

# =====================
# Compile rule
# =====================
$(BUILD_DIR)/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

# =====================
# Clean
# =====================
clean:
	rm -rf $(BUILD_DIR)

.PHONY: all benchmark clean