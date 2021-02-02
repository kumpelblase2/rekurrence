# rekurrence

A kotlin/JVM library to parse and handle RFC 5545 recurrence rules.

**Note: This does not handle everything 100% correct yet.**

This library focuses on using the existing java 8 date-time api instead of using their own date/time objects like other libraries do.
Initially, this project was created for my [focus](https://kumpelblase2/focus) project, but is very much separate and can be used completely independently.
It can be used to parse and create RFC 5545 recurrence rule strings (e.g. `FREQ=WEEKLY;BYDAY=MO`) but also generate the occurrences for such rules.

The provided API is very small:
- to parse a rule string: `RecurringRule.fromString()`
- to turn a rule into a string: `rule.asString()`
- to generate next occurrences: `rule.getNextEntries()`
- and a builder for creating rules: `RecurringRule.Builder().build()`
