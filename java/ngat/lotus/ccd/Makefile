# Makefile
# $HeadURL$
include ../../../../Makefile.common
include ../../../../../Makefile.common

MAKE 		= make
PACKAGEDIR	= ngat/lotus/ccd
DIRS 		= 
BINDIR 		= $(LOTUS_BIN_HOME)/java/$(PACKAGEDIR)
SRCS 		= LOTUSINDIConnection.java  LOTUSINDIDevice.java  StarlightExpressTriusSX35.java
OBJS 		= $(SRCS:%.java=$(BINDIR)/%.class)
# The INDI libraries need at least Java v1.7
# This javac is on ltobs9, you may need to change this for your machine
JAVAC		= /usr/java/jdk1.7.0_67/bin/javac

top: dirs objs

dirs:
	@for i in $(DIRS); \
	do \
		(echo making in $$i...; cd $$i; $(MAKE) ); \
	done;

objs: $(OBJS)

$(BINDIR)/%.class:%.java
	$(JAVAC) -sourcepath .:$(LOTUS_SRC_HOME)/java \
		-classpath $(LOTUS_BIN_HOME)/java:$(CLASSPATH) $(JAVAC_OPTIONS) -d $(LOTUS_BIN_HOME)/java $<

depend:
	echo no depend in ngat/lotus/ccd directory.

clean: tidy
	-$(RM) $(RM_OPTIONS) $(OBJS)
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
