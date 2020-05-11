package com.maze.server

package object processor {

  sealed trait ProcessorCommand
  case class UsersProcessClean() extends ProcessorCommand
}
