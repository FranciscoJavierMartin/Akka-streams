package part1_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer()

  // sources
  val source = Source(1 to 10)

  // sinks
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
  //graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x+1)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

  sourceWithFlow.to(sink).run()
  source.to(flowWithSink).run()
  source.via(flow).to(sink).run()

  // nulls are NOT allowed
  // val illegalSource = Source.single[String](null)
  // illegalSource.to(Sink.foreach(println)).run()

  // Use Options instead

  // various kinds of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(Stream.from(1))

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture((Future(42)))

  // sinks
  val theMostBoringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream
  val foldSink = Sink.fold[Int,Int](0)((a,b)=>a+b)

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(x => 2*x)
  val takeFlow = Flow[Int].take(5)
  // drop, filter, but not flatMap

  // source -> flow -> flow -> ... -> sink
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  doubleFlowGraph.run()

  // syntactic sugars
  val mapSource = Source(1 to 10).map(x => x*2)
  mapSource.runForeach(println)

  // Operators = components
  /**
    * Exercise: Create a stream that takes the name of people, then you will keep the first two names with length > 5 characters and print it
    */
  Source(List("Alices", "Bob", "Catherine", "Dan", "Esther"))
    .filter(name => name.length > 5)
    .take(2)
    .runForeach(println)

}
