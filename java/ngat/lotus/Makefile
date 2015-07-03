# Makefile
# $Header$
include ../../../Makefile.common
include ../../../../Makefile.common

MAKE 		= make
PACKAGEDIR	= ngat/lotus/
BINDIR 		= $(LOTUS_BIN_HOME)/java/$(PACKAGEDIR)
CONFIG_BINDIR 	= $(LOTUS_BIN_HOME)/java
SCRIPT_BINDIR 	= $(LOTUS_BIN_HOME)/java
SRCS 		= $(MAIN_SRCS) $(IMPL_SRCS)
MAIN_SRCS 	= LOTUSConstants.java \
		LOTUS.java LOTUSStatus.java LOTUSTCPClientConnectionThread.java \
		LOTUSTCPServerConnectionThread.java LOTUSTCPServer.java LOTUSLogFormatter.java \
		LOTUSREBOOTQuitThread.java 
IMPL_SRCS = $(BASE_IMPL_SRCS) $(CALIBRATE_IMPL_SRCS) $(EXPOSE_IMPL_SRCS) $(INTERRUPT_IMPL_SRCS) $(SETUP_IMPL_SRCS)
BASE_IMPL_SRCS		= CommandImplementation.java JMSCommandImplementation.java UnknownCommandImplementation.java \
			HardwareImplementation.java FITSImplementation.java
CALIBRATE_IMPL_SRCS 	= 
EXPOSE_IMPL_SRCS	= EXPOSEImplementation.java MULTRUNImplementation.java
INTERRUPT_IMPL_SRCS 	= INTERRUPTImplementation.java ABORTImplementation.java GET_STATUSImplementation.java \
			REBOOTImplementation.java

SETUP_IMPL_SRCS		= SETUPImplementation.java CONFIGImplementation.java
OBJS 		= $(SRCS:%.java=$(BINDIR)/%.class)

CONFIG_SRCS	= lotus1.lotus.net.properties lotus1.lotus.properties lotus1.fits.properties \
		lotus2.lotus.net.properties lotus2.lotus.properties lotus2.fits.properties 
CONFIG_OBJS	= $(CONFIG_SRCS:%.properties=$(CONFIG_BINDIR)/%.properties)
SCRIPT_SRCS	= 
SCRIPT_OBJS	= $(SCRIPT_SRCS:%=$(SCRIPT_BINDIR)/%)
DIRS 		= ccd

top: dirs $(OBJS) config scripts dirs

#
# Java classes
#
$(BINDIR)/%.class:%.java
	$(JAVAC) -sourcepath .:$(LOTUS_SRC_HOME)/java \
		-classpath $(LOTUS_BIN_HOME)/java:$(CLASSPATH) $(JAVAC_OPTIONS) -d $(LOTUS_BIN_HOME)/java $<

#
# configuration files
#
config: $(CONFIG_OBJS) $(INI_OBJS)


$(CONFIG_BINDIR)/%.properties: %.properties
	$(CP) $< $@

#
# run scripts
#
scripts: $(SCRIPT_OBJS)

#
# sub dirs
#
dirs:
	@for i in $(DIRS); \
	do \
		(echo making in $$i...; cd $$i; $(MAKE) ); \
	done;

depend:
	echo no depend in ngat directory.

clean: tidy
	-$(RM) $(RM_OPTIONS) $(OBJS)
	-$(RM) $(RM_OPTIONS) $(JARLIBDIR)/$(JAR_FILE)
	@for i in $(DIRS); \
	do \
		(echo clean in $$i...; cd $$i; $(MAKE) clean); \
	done;

tidy:
	-$(RM) $(RM_OPTIONS) $(TIDY_OPTIONS)
	@for i in $(DIRS); \
	do \
		(echo tidy in $$i...; cd $$i; $(MAKE) tidy); \
	done;

backup: tidy
	@for i in $(DIRS); \
	do \
		(echo backup in $$i...; cd $$i; $(MAKE) backup); \
	done;
