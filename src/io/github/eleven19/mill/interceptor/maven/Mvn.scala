package io.github.eleven19.mill.interceptor.maven
import caseapp.* 
import kyo.* 

object Mvn:
    def run(args: Chunk[String]) = direct {
        CaseApp.detailedParseWithHelp[Options](args.toList) match
            case Left(error) =>
                Console.printLine(s"Error: $error").now
            case Right((Left(error), helpAsked, usageAsked, remainingArgs)) =>
                Console.printLine(s"Error: $error").now
                Console.printLine(s"Help asked: $helpAsked").now
                Console.printLine(s"Usage asked: $usageAsked").now
                Console.printLine(s"Remaining args: $remainingArgs").now
            case Right((Right(options), helpAsked, usageAsked, remainingArgs)) =>
                Console.printLine(s"Options: $options").now
                Console.printLine(s"Help asked: $helpAsked").now
                Console.printLine(s"Usage asked: $usageAsked").now
                Console.printLine(s"Remaining args: $remainingArgs").now
    } 

    final case class Options(
    )
