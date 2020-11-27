/**
 * This source file is part of Cell.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cell.core.cellet;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.TalkError;
import cell.util.log.Logger;

/**
 * 仅用于技术验证的哑元。
 */
public class Dummy extends Cellet {

	public Dummy() {
		super("Dummy");
	}

	@Override
	public boolean install() {
		Logger.i(this.getClass(), getName() + " install"); 
		return true;
	}

	@Override
	public void uninstall() {
		Logger.i(this.getClass(), getName() + " uninstall"); 
	}

	@Override
	public void onListened(TalkContext context, Primitive primitive) {
		Logger.i(this.getClass(), "onListened - stuff num : " + primitive.numStuff());
	}

	@Override
	public void onSpoke(TalkContext context, Primitive primitive) {
		Logger.i(this.getClass(), "onSpoke - stuff num : " + primitive.numStuff());
	}

	@Override
	public void onAck(TalkContext context, Primitive primitive) {
		Logger.i(this.getClass(), "onAck - stuff num : " + primitive.numStuff());
	}

	@Override
	public void onSpeakTimeout(TalkContext context, Primitive primitive) {
		Logger.i(this.getClass(), "onSpeakTimeout - stuff num : " + primitive.numStuff());
	}

	@Override
	public void onContacted(TalkContext context, Servable server) {
		Logger.i(this.getClass(), "onContacted - session id : " + context.getSessionId());
	}

	@Override
	public void onQuitted(TalkContext context, Servable server) {
		Logger.i(this.getClass(), "onQuitted - session id : " + context.getSessionId());
	}

	@Override
	public void onFailed(TalkError fault) {
		Logger.i(this.getClass(), "onFailed - error code : " + fault.getErrorCode());
	}

}
