package henplus;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.DefaultParser;

/**
 * A jline extension for SQL-like inputs. Commands are terminated with a semicolon.
 */
public final class HenplusLineParser implements Parser {

  private static final Parser DEFAULT_PARSER = new DefaultParser();
  private CommandDispatcher _dispatcher;

  @Override
  public ParsedLine parse(String line, int cursor, ParseContext context) throws SyntaxError {
    if ((ParseContext.UNSPECIFIED.equals(context) || ParseContext.ACCEPT_LINE.equals(context))) {
      final Command c = _dispatcher.getCommandFrom(line.trim());
      if ((c != null && !c.isComplete(line.trim())) || (c == null && !line.trim().endsWith(";"))) {
        throw new EOFError(-1, cursor, "Missing semicolon (;)");
      }
    }

    return DEFAULT_PARSER.parse(line, cursor, context);
  }

  public void dispatcher(CommandDispatcher dispatcher) {
    _dispatcher = dispatcher;
  }
}
