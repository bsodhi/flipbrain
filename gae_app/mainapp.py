"""
Copyright 2017 Balwinder Sodhi

Licenced under MIT Licence as available here:
https://opensource.org/licenses/MIT

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

Created on Mar 3, 2017

@author: Balwinder Sodhi
"""

import webapp2
from flipbrain import users
from flipbrain import trails
from flipbrain import assessments
from flipbrain.common import MyWarmupHandler

config = {}
config['webapp2_extras.sessions'] = {
    'secret_key': 'HJ89yu6643GfTAC$rT670YtR1iaIl',
}

'''
Routing for all action URLs ending in .a
'''
app = webapp2.WSGIApplication([
    ('/_ah/warmup', MyWarmupHandler),
    (r'/user/(\w+)\.a', users.UserHandler),
    (r'/trail/(\w+)\.a', trails.TrailHandler),
    (r'/assess/(\w+)\.a', assessments.AssessmentHandler),
], debug=True, config=config)
