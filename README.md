# Substrate

Opinionated Aeron API

## Goals

- Productive, easy to configure Aeron, suitable for typical use cases
- Hide complexity of Aeron
- Support IPC, UDP and Archive
- No support for Aeron Cluster or advanced features

## When to avoid

- when direct control over streams are required;
- when maximum performance is required;
- when independent FragmentHandlers are required for different streams within a single agent;
- when full control is required over duty cycles and the agent infrastructure;
- when total control over messaging formats is required.

## Done

Not much

## Todo

* AgentRunner
* CompositeAgent
* Archive Support (server and replay)
* Header - encode and decode
* Wiring up
* Direct channel configuration
* Connectivity checking
* Disconnect notification
* Heartbeats
* Timer?