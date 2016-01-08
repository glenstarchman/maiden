package com.maiden.common


object Enums {

  object DayOfWeek extends Enumeration {
    type DayOfWeek = Value
    val Sunday = Value(0, "Sunday")
    val Monday = Value(1, "Monday")
    val Tuesday = Value(2, "Tuesday")
    val Wednesday = Value(3, "Wednesday")
    val Thursday = Value(4, "Thursday")
    val Friday = Value(5, "Friday")
    val Saturday = Value(6, "Saturday")

  }

  object FareType extends Enumeration {
    type FareType = Value
    val Fixed = Value(1, "Fixed")
    val OnDemand = Value(2, "On Demand")
    val Discounted = Value(3, "Discounted")
  }

  object StopType extends Enumeration {
    type StopType = Value
    val Bar = Value(1, "Bar")
    val Restaurant =  Value(2, "Restaurant")
    val Street = Value(3, "Street")
    val Hotel = Value(4, "Hotel")
    val Airport = Value(5, "Airport")
    val Casino = Value(6, "Casino")
  }

  object VehicleType extends Enumeration {
    type VehicleType = Value
    val Bus = Value(1, "Bus")
    val Towncar = Value(2, "Town Car")
    val SUV = Value(3, "SUV")
    val Limo = Value(4, "Limousine")
    val Economy = Value(5, "Economy")
  } 

  object ReservationType extends Enumeration {
    type ReservationType = Value
    val OnDemand = Value(1, "On Demand")
    val Reserved = Value(2, "Reserved")
  }

  object RideStateType extends Enumeration {
    type RideStateType = Value
    //successful states
    val Initial = Value(1, "Initial")
    val FindingVehicle = Value(2, "Finding Vehicle")
    val VehicleAccepted = Value(3, "Vehicle Accepted")
    val VehicleOnWay = Value(4, "Vehicle On Way")
    val RideUnderway = Value(5, "Ride Underway")
    val RideCompleted = Value(6, "Ride Completed")
    val RideRebooked = Value(7, "Rebooked")

    //exceptions
    val DriverCancelled = Value(-1, "Driver Cancelled")
    val PassengerCancelled = Value(-2, "Passenger Cancelled")
    val PassengerCancelledWithPenalty = Value(-3, "Passenger Cancelled With Penalty")
    val PassengerNoShow = Value(-4, "Passenger No Show")
    val PassengerNoShowWithPenalty = Value(-5, "Passenger No Show With Penalty")
    val DriverNoShow = Value(-6, "Driver No Show")
    val NoDriverFound = Value(-7, "No Drivers Found")
    val OutOfServiceArea = Value(-8, "Out of Service Area")
    val ReservationTimeout = Value(-9, "Reservation Timed Out")
    val NoAvailableVehicles = Value(-10, "No Available Vehicles")
    val AlreadyBooked = Value(-11, "User Already has a Ride")

    //super exceptional cases
    //these are for cases where the ride was terminated
    //either because the passenger was unruly or the driver had 
    //to cancel mid-ride
    val RideTerminatedNoCharge = Value(-12, "Ride Terminated Without Charge")
    val RideTerminatedWithPenalty = Value(-13, "Ride Terminated With Penalty")
   val OutsideOperatingHours = Value(-14, "Reservation Attempted Outside of Operating Hours")

  }

  object PaymentStateType extends Enumeration {
    type PaymentStateType = Value
    val Pending = Value(1, "Pending")
    val Success = Value(2, "Success")
    val CompedByDriver = Value(3, "Comped By Driver")
    val CompedByCompany = Value(4, "Comped By Company")
    val CompedByPromoCode = Value(5, "Comped By Promo Code")
    val TransferNoCharge = Value(6, "Transfer (No Charge)")

    //signal to retry in 24 hours
    val FailureRetry = Value(-1, "Failure (Retry Pending")
    val Failure = Value(-2, "Failure")
  }

  object DiscountType extends Enumeration {
    type DiscountType = Value
    val PromoCode = Value(1, "Promo Code")
    val PreferredCustomer = Value(2, "Preffered Customer")
    val MonthlyMembership = Value(3, "Monthly Membership")
    val DayPass = Value(4, "Day Pass")
    val NoDiscount = Value(5, "No Discount")
    val WeekendPass = Value(6, "Weekend Pass")
  }

  object PassType extends Enumeration {
    type PassType = Value
    val Monthly = Value(1, "Monthly")
    val Yearly = Value(2, "Yearly")
    val Weekend = Value(3, "Weekend")
    val Day = Value(4, "Day")
  } 

  object OsrmInstructionType extends Enumeration {
    type OsrmInstructionType = Value
    val NoTurn = Value(0, "No turn")
    val GoStraight = Value(1, "Go straight")
    val TurnSlightRight = Value(2, "Go slight right")
    val TurnRight = Value(3, "Turn right")
    val TurnSharpRight = Value(4, "Turn sharp right")
    val UTurn = Value(5, "U-turn")
    val TurnSharpLeft = Value(6, "Turn sharp left")
    val TurnLeft = Value(7, "Turn left")
    val TurnSlightLeft = Value(8, "Turn slight left")
    val ReachViaLocation = Value(9, "Reach via location")
    val Head = Value(10, "Head")
    val EnterRoundabout = Value(11, "Enter roundabout")
    val LeaveRoundabout = Value(12, "Leave roundabout")
    val StayOnRoundabout = Value(13, "Stay on roundabout")
    val StartAtEndOfStreet = Value(14, "Start at end of street")
    val ReachedDestination = Value(15, "Reached destination")
  }


}
