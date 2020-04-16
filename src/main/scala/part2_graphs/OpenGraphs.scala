package part2_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source}

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
    A composite source that concatenates 2 sources
    - emits all the elements from the first source
    - then all the elements from the second
   */

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 100)

  // step 1
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2: declaring components
      val concat = builder.add(Concat[Int](2))

      // step 3: tying them together
      firstSource ~> concat
      secondSource ~> concat

      // step 4
      SourceShape(concat.out)
    }
  )

  sourceGraph.to(Sink.foreach(println)).run()

  /*
    Complex sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  // step 1
  val sinkGraph = Sink.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - add a broadcast
      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      // step 4 - return the shape
      SinkShape(broadcast.in)
    }
  )

  firstSource.to(sinkGraph).run()

  /**
    * Challenge - complex flow?
    * Write your onw flow that's composed of two other flows
    * - one that adds 1 to a number
    * - one that does number * 10
    */

  val incrementer = Flow[Int].map(_ +1)
  val multiplier = Flow[Int].map(_*10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      // everything operates on shapes
      // step 2 - define auxiliary components
      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)
      // step 3 - connect the components
      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out) // shape
    } // static graph
  ) // component

  firstSource.via(flowGraph).to(Sink.foreach(println)).run()

  /**
    * Exercise: flow from a sink and a source?
    */

  def fromSinkAndSource[A,B](sink: Sink[A,_], source: Source[B,_]): Flow[A,B,_] =
    Flow.fromGraph(
      GraphDSL.create(){implicit  builder =>
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)

        FlowShape(sinkShape.in, sourceShape.out)
      }
    )

  val f = Flow.fromSinkAndSource(Sink.foreach[String](println), Source(1 to 10))
}
