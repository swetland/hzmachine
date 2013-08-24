
# Set this to color or gray
#
SCREEN     ?= color

# Use this line to build against the released SDK
#
#SDKHOME    := /home/swetland/sdk-40194

# Use this line to build against a check out from 
# revision control.  Not useful for non-Danger people.
#
#TREE       := /home/swetland/exp/platform


# -----------------------------------------------------------------------
#
# Apps needed for the build.
#
MD5SUM     := md5sum
JAVAC      := javac
JAVA       := java

# -----------------------------------------------------------------------
#
# The bits from here down should not need to be modified...
#
#
ifneq ($(TREE),)
LIBRARY    := $(TREE)/libdanger/classes
TOOLS      := $(TREE)/bin
LINK       := $(TREE)/target/libs/library.link
else
ERROR      := You must set SDKHOME
endif

ifneq ($(SDKHOME),)
TOOLS      := $(SDKHOME)/tools/linux
LIBRARY    := $(SDKHOME)/libs/library.jar
LINK       := $(SDKHOME)/libs/library.link
LIBS       := $(SDKHOME)/libs
ERROR      :=
endif

MKBUNDLE   := $(TOOLS)/mkbundle
DRC        := $(TOOLS)/drc 

ifeq ($(APPNAME),)
ERROR      := You must set APPNAME
endif

ifneq ($(ERROR),)
all:
	@echo "*** ERROR: $(ERROR)"
else

ifeq ($(SCREEN),color)
SIMFLAGS   := -Dcom.danger.screen.color_space=color16
else
SIMFLAGS   := -Dcom.danger.screen.color_space=gray
endif

JFLAGS     := -classpath $(LIBRARY):. -d classes

BUNDLE     := $(APPNAME).bndl
RSRC_SRC   := $(APPNAME).rsrc
RSRC_DB    := $(APPNAME)-$(SCREEN).rdb

# SRCS must be all source files, including the INTERFACES
# which may or may not exist in the filesystem when we
# first do a build (because clean removes them)
#
SRCS       := $(shell find . -name \*.java) 
SRCS       := $(filter-out $(addprefix ./, $(INTERFACES)),$(SRCS))
SRCS       += $(INTERFACES)

# javac likes to figure out its own depend stuff but invariably screws
# stuff up for me -- let's just find all the java files and force a
# rebuild if the list of files changes in any way (files added, removed,
# etc).  When we build, we nuke the classes/ directory so that we don't
# suffer when we remove a .java file and the .class file lingers
#
STAMP      := $(shell find . -name \*.java | $(MD5SUM) | sed 's/.\ .*//g')
STAMPFILE  := stamp.$(STAMP)

all: $(STAMPFILE) $(APPNAME).bndl

$(STAMPFILE): $(SRCS) $(RSRC_DB)
	@rm -rf classes stamp.* 
	@mkdir -p classes
	$(JAVAC) $(JFLAGS) $(SRCS)
	$(MKBUNDLE) -o classes/application.dat $(RSRC_DB)
	@touch $(STAMPFILE)

$(INTERFACES): $(RSRC_SRC)
	$(DRC) -i $<

%-gray.rdb: %.rsrc
	$(DRC) -i $< -o $@ 

%-color.rdb: %.rsrc
	$(DRC) -C -i $< -o $@ 

ifneq ($(BUNDLENAME),)
SIMFLAGS += -Dcom.danger.autoboot=$(BUNDLENAME)
endif

SIMCLASSPATH := -classpath $(LIBS)/simulator.jar:$(LIBS)/library.jar:classes

run: $(STAMPFILE) 
	$(JAVA) $(SIMFLAGS) $(SIMCLASSPATH) danger.Boot 

run-gray: $(STAMPFILE) 
	$(JAVA) -Dcom.danger.screen.color_space=gray $(SIMFLAGS) $(SIMCLASSPATH) danger.Boot 

%.bndl: $(STAMPFILE) $(RSRC_DB)
	$(MKBUNDLE) -o $@ $(LINK) classes $(RSRC_DB) -l $(APPNAME).lst 
ifneq ($(TREE),)
	cp -f $@ $(TREE)/target
endif

clean::
	rm -rf classes $(INTERFACES) *~ *.rdb *.bndl *.lst stamp.* 

endif
