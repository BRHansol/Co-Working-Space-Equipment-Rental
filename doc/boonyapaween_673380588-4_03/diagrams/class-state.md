# Class Diagram: State Pattern ของการจอง

![Class Diagram: State Pattern](png/class-state.png)

ซอร์ส PlantUML: [class-state.puml](class-state.puml)

| บทบาทใน State Pattern | คลาส |
|---|---|
| State | `BookingState` |
| Concrete State | `PendingState`, `ApprovedState`, `RejectedState`, `CancelledState`, `CompletedState` |
| Context | `BookingContext` |
| Client | `BookingServiceImpl` |
