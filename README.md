[![Build Status](https://travis-ci.org/ITDSystems/alvex-business-calendar.svg?branch=master)](https://travis-ci.org/ITDSystems/alvex-business-calendar)

Alvex business calendar component
================================

This component provides a mechanism to automatically set task due dates based on predefined task execution limits. Task due date is set with accordance to business calendar.

![image](https://github.com/ITDSystems/alvex/blob/master/img/alvex-configure-due-dates.png?raw=true)


Compatible with Alfresco 5.x.

# Customization

This component works out of the box and uses holiday calendar for Russia by default. To customize it one should implement [BusinessCalendarHandler](https://github.com/ITDSystems/alvex-business-calendar/blob/master/repo/src/main/java/com/alvexcore/repo/bcal/BusinessCalendarHandler.java) interface and define the corresponding bean that **must** use `alvexBusinessCalendarAbstractHandler` as a parent one. For more details see [CustomBusinessCalendarHandler](https://github.com/ITDSystems/alvex-business-calendar/blob/master/repo/src/main/java/com/alvexcore/repo/bcal/CustomBusinessCalendarHandler.java) implementation and [bean](https://github.com/ITDSystems/alvex-business-calendar/blob/master/repo/src/main/amp/config/alfresco/module/business-calendar/context/alvex-business-calendar-context.xml#L14) definition.

# Downloads

Download ready-to-use Alvex components via [Alvex](https://github.com/ITDSystems/alvex#downloads).

# Build from source

To build Alvex follow [this guide](https://github.com/ITDSystems/alvex#build-component-from-source).

