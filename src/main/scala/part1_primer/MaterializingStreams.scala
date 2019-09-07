package part1_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val simpleGraph = Source(1 to 0).to(Sink.foreach(println))
  //val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a,b)=>a+b)
  val sumFuture = source.runWith(sink)
  sumFuture.onComplete {
    case Success(value) => println(s"The sum of all elements is: $value")
    case Failure(exception) => println(s"The sum os the elements could not be computed: $exception")
  }

  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x+1)
  val simpleSink = Sink.foreach[Int](println)
  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  graph.run().onComplete {
    case Success(_) => "Stream processing finished"
    case Failure(exception) => s"Stream processing failed with: $exception"
  }

  // sugars
  Source(1 to 10).runWith(Sink.reduce[Int](_+_))
  Source(1 to 10).runReduce(_+_) // same

  // backwards
  Sink.foreach[Int](println).runWith(Source.single(42))

  // both ways
  Flow[Int].map(x => 2*x).runWith(simpleSource, simpleSink)

  /**
    * - Return the last element out od a source (use Sink.last)
    * - Compute the total word count out of a stream of sentences
    */

  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  val senteceSource = Source(List(
      "Harry Potter and the philosofer stone",
      "Titanic",
      "Harry Potter and the chamber of secrets",
      "The old man and the sea"
    ))

  val wordCountSink = Sink.fold[Int,String](0)((currentWords, newSentence)=> currentWords + newSentence.split(" ").length)
  val g1 = senteceSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = senteceSource.runWith(wordCountSink)
  val g3 = senteceSource.runFold(0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)

  val wordCountFlow = Flow[String].fold[Int](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g4 = senteceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
  val g5 = senteceSource.viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right).run()
  val g6 = senteceSource.via(wordCountFlow).runWith(Sink.head)
  val g7 = wordCountFlow.runWith(senteceSource, Sink.head)._2

}
