/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */

package com.nickelsoftware.bettercare4me.utils

import com.datastax.driver.core.{Row, BoundStatement, ResultSet, ResultSetFuture}
import scala.concurrent.{CanAwait, Future, ExecutionContext}
import scala.util.{Success, Try}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

/**
 * To provide implicit conversion of ResultSetFuture into Scala's Future[ResultSet]. 
 * This allows to use the Future.map method to turn the ResultSet into List[A]
 * 
 * from https://github.com/eigengo/activator-akka-cassandra/blob/master/src/main/scala/core/cassandra.scala
 * see http://www.cakesolutions.net/teamblogs/2013/07/31/akka-cassandra-activator
 */
private[utils] trait CassandraResultSetOperations {
  private case class ExecutionContextExecutor(executionContext: ExecutionContext) extends java.util.concurrent.Executor {
    def execute(command: Runnable): Unit = { executionContext.execute(command) }
  }

  protected class RichResultSetFuture(resultSetFuture: ResultSetFuture) extends Future[ResultSet] {
    @throws(classOf[InterruptedException])
    @throws(classOf[scala.concurrent.TimeoutException])
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
      resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
      this
    }

    @throws(classOf[Exception])
    def result(atMost: Duration)(implicit permit: CanAwait): ResultSet = {
      resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
    }

    def onComplete[U](func: (Try[ResultSet]) => U)(implicit executionContext: ExecutionContext): Unit = {
      if (resultSetFuture.isDone) {
        func(Success(resultSetFuture.getUninterruptibly))
      } else {
        resultSetFuture.addListener(new Runnable {
          def run() {
            func(Try(resultSetFuture.get()))
          }
        }, ExecutionContextExecutor(executionContext))
      }
    }

    def isCompleted: Boolean = resultSetFuture.isDone

    def value: Option[Try[ResultSet]] = if (resultSetFuture.isDone) Some(Try(resultSetFuture.get())) else None
  }

  implicit def toFuture(resultSetFuture: ResultSetFuture): Future[ResultSet] = new RichResultSetFuture(resultSetFuture)
}

trait Binder[-A] {

  def bind(value: A, boundStatement: BoundStatement): Unit

}

trait BoundStatementOperations {

  implicit class RichBoundStatement[A : Binder](boundStatement: BoundStatement) {
    val binder = implicitly[Binder[A]]

    def bindFrom(value: A): BoundStatement = {
      binder.bind(value, boundStatement)
      boundStatement
    }
  }

}

object cassandra {

  object resultset extends CassandraResultSetOperations

  object boundstatement extends BoundStatementOperations

}