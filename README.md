**This extension for Alfresco is obsolete and unsupported. Use it on your own risk.**

[![Build Status](https://travis-ci.org/ITDSystems/alvex-business-calendar.svg?branch=master)](https://travis-ci.org/ITDSystems/alvex-business-calendar)

Alvex business calendar component
================================

This component provides a mechanism to automatically set task due dates based on predefined task execution limits. Task due date is set with accordance to business calendar.

![image](https://github.com/ITDSystems/alvex/blob/master/img/alvex-configure-due-dates.png?raw=true)

Compatible with Alfresco 5.1 and 5.2.

This component requires:
* [Alvex Utils](https://github.com/ITDSystems/alvex-utils)
* [net.objectlab.kit.datecalc.jdk8](http://objectlabkit.sourceforge.net/apidocs/net/objectlab/kit/datecalc/jdk8/package-summary.html)
* [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)

# Using this project

Recommended way to use Alvex components is to include them as dependencies to your Maven project. Follow [this guide](https://github.com/ITDSystems/alvex#recommended-way-include-alvex-to-your-project-via-maven-configuration) to include this component to your project.

# Build from source

To build Alvex follow [this guide](https://github.com/ITDSystems/alvex#build-component-from-source).

# Quick Start

This component works out of the box and uses holiday calendar for Russia by default. To customize it one should provide custom implementation of calendar handler that extends [AbstractBusinessCalendarHandler](https://github.com/ITDSystems/alvex-business-calendar/blob/master/repo/src/main/java/com/alvexcore/repo/bcal/AbstractBusinessCalendarHandler.java) class and define the corresponding bean that **must** use `alvexBusinessCalendarAbstractHandler` as a parent one. For more details see [CustomBusinessCalendarHandler](https://github.com/ITDSystems/alvex-business-calendar/blob/master/repo/src/main/java/com/alvexcore/repo/bcal/CustomBusinessCalendarHandler.java) implementation and [bean](https://github.com/ITDSystems/alvex-business-calendar/blob/master/repo/src/main/amp/config/alfresco/module/business-calendar/context/alvex-business-calendar-context.xml#L14) definition.
